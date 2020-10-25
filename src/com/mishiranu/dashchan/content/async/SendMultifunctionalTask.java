package com.mishiranu.dashchan.content.async;

import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.Chan;
import chan.content.ChanPerformer;
import chan.content.ExtensionException;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpHolder;
import com.mishiranu.dashchan.content.model.ErrorItem;
import com.mishiranu.dashchan.content.model.PostNumber;
import java.util.ArrayList;
import java.util.List;

public class SendMultifunctionalTask extends HttpHolderTask<Void, Boolean> {
	private final State state;
	private final String type;
	private final String text;
	private final ArrayList<String> options;
	private final Callback callback;

	private String archiveBoardName;
	private String archiveThreadNumber;
	private ErrorItem errorItem;

	public enum Operation {DELETE, REPORT, ARCHIVE}

	public interface Callback {
		void onSendSuccess(State state, String archiveBoardName, String archiveThreadNumber);
		void onSendFail(State state, String type, String text, ArrayList<String> options, ErrorItem errorItem);
	}

	public static final String OPTION_FILES_ONLY = "filesOnly";

	public static class State {
		public final Operation operation;
		public final Chan chan;
		public final String boardName;
		public final String threadNumber;

		public List<Pair<String, String>> types;
		public List<Pair<String, String>> options;

		public boolean commentField;

		public List<PostNumber> postNumbers;
		public String archiveThreadTitle;
		public Chan archiveChan;

		public State(Operation operation, Chan chan, String boardName, String threadNumber,
				List<Pair<String, String>> types, List<Pair<String, String>> options, boolean commentField) {
			this.operation = operation;
			this.chan = chan;
			this.boardName = boardName;
			this.threadNumber = threadNumber;
			this.types = types;
			this.options = options;
			this.commentField = commentField;
		}

		private Chan getWorkChan() {
			return archiveChan != null ? archiveChan : chan;
		}
	}

	public SendMultifunctionalTask(Callback callback,
			State state, String type, String text, ArrayList<String> options) {
		super(state.getWorkChan());
		this.state = state;
		this.type = type;
		this.text = text;
		this.options = options;
		this.callback = callback;
	}

	private static List<String> createPostNumberList(List<PostNumber> numbers) {
		ArrayList<String> postNumbers = new ArrayList<>(numbers.size());
		for (PostNumber number : numbers) {
			postNumbers.add(number.toString());
		}
		return postNumbers;
	}

	@Override
	protected Boolean run(HttpHolder holder) {
		Chan chan = state.chan;
		try {
			switch (state.operation) {
				case DELETE: {
					chan.performer.safe().onSendDeletePosts(new ChanPerformer
							.SendDeletePostsData(state.boardName, state.threadNumber,
							createPostNumberList(state.postNumbers), text,
							options != null && options.contains(OPTION_FILES_ONLY), holder));
					break;
				}
				case REPORT: {
					chan.performer.safe().onSendReportPosts(new ChanPerformer
							.SendReportPostsData(state.boardName, state.threadNumber,
							createPostNumberList(state.postNumbers), type, options, text, holder));
					break;
				}
				case ARCHIVE: {
					Uri uri = chan.locator.safe(false)
							.createThreadUri(state.boardName, state.threadNumber);
					if (uri == null) {
						errorItem = new ErrorItem(ErrorItem.Type.UNKNOWN);
						return false;
					}
					Chan archiveChan = state.archiveChan;
					if (archiveChan == null) {
						errorItem = new ErrorItem(ErrorItem.Type.UNKNOWN);
						return false;
					}
					chan = archiveChan;
					ChanPerformer.SendAddToArchiveResult result = chan.performer.safe()
							.onSendAddToArchive(new ChanPerformer.SendAddToArchiveData(uri, state.boardName,
									state.threadNumber, options, holder));
					if (result != null && result.threadNumber != null) {
						archiveBoardName = result.boardName;
						archiveThreadNumber = result.threadNumber;
					}
					break;
				}
			}
			return true;
		} catch (ExtensionException | HttpException | InvalidResponseException e) {
			errorItem = e.getErrorItemAndHandle();
			return false;
		} catch (ApiException e) {
			errorItem = e.getErrorItem();
			return false;
		} finally {
			chan.configuration.commit();
		}
	}

	@Override
	protected void onComplete(Boolean success) {
		if (success) {
			callback.onSendSuccess(state, archiveBoardName, archiveThreadNumber);
		} else {
			callback.onSendFail(state, type, text, options, errorItem);
		}
	}
}
