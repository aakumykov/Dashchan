package com.mishiranu.dashchan.ui.posting.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.mishiranu.dashchan.R;
import com.mishiranu.dashchan.ui.SeekBarForm;
import com.mishiranu.dashchan.util.GraphicsUtils;
import com.mishiranu.dashchan.util.ResourceUtils;
import com.mishiranu.dashchan.widget.ThemeEngine;
import java.util.Locale;

public class ReencodingDialog extends DialogFragment implements DialogInterface.OnClickListener,
		RadioGroup.OnCheckedChangeListener {
	public static final String TAG = ReencodingDialog.class.getName();

	private static final String EXTRA_QUALITY = "quality";
	private static final String EXTRA_REDUCE = "reduce";

	private RadioGroup radioGroup;
	private SeekBarForm qualityForm;
	private SeekBarForm reduceForm;

	private static final String[] OPTIONS = {GraphicsUtils.Reencoding.FORMAT_JPEG.toUpperCase(Locale.US),
			GraphicsUtils.Reencoding.FORMAT_PNG.toUpperCase(Locale.US)};
	private static final String[] FORMATS = {GraphicsUtils.Reencoding.FORMAT_JPEG,
			GraphicsUtils.Reencoding.FORMAT_PNG};
	private static final int[] IDS = {android.R.id.icon1, android.R.id.icon2};

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context context = getActivity();
		qualityForm = new SeekBarForm(false);
		qualityForm.setConfiguration(1, 100, 1, 1);
		qualityForm.setValueFormat(ResourceUtils.getColonString(getResources(), R.string.quality, "%d%%"));
		qualityForm.setCurrentValue(savedInstanceState != null ? savedInstanceState.getInt(EXTRA_QUALITY) : 90);
		reduceForm = new SeekBarForm(false);
		reduceForm.setConfiguration(1, 8, 1, 1);
		reduceForm.setValueFormat(ResourceUtils.getColonString(getResources(), R.string.reduce, "%dx"));
		reduceForm.setCurrentValue(savedInstanceState != null ? savedInstanceState.getInt(EXTRA_REDUCE) : 1);
		int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding_view);
		View qualityView = qualityForm.inflate(context);
		qualityForm.getSeekBar().setSaveEnabled(false);
		qualityView.setPadding(qualityView.getPaddingLeft(), 0, qualityView.getPaddingRight(), padding / 2);
		View reduceView = reduceForm.inflate(context);
		reduceForm.getSeekBar().setSaveEnabled(false);
		reduceView.setPadding(reduceView.getPaddingLeft(), 0, reduceView.getPaddingRight(),
				reduceView.getPaddingBottom());
		radioGroup = new RadioGroup(context);
		radioGroup.setOrientation(RadioGroup.VERTICAL);
		radioGroup.setPadding(padding, padding, padding, padding / 2);
		radioGroup.setOnCheckedChangeListener(this);
		for (int i = 0; i < OPTIONS.length; i++) {
			RadioButton radioButton = new RadioButton(context);
			ThemeEngine.applyStyle(radioButton);
			radioButton.setText(OPTIONS[i]);
			radioButton.setId(IDS[i]);
			radioGroup.addView(radioButton);
		}
		radioGroup.check(IDS[0]);
		LinearLayout linearLayout = new LinearLayout(context);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		FrameLayout qualityLayout = new FrameLayout(context);
		qualityLayout.setId(android.R.id.text1);
		qualityLayout.addView(qualityView);
		FrameLayout reduceLayout = new FrameLayout(context);
		reduceLayout.setId(android.R.id.text2);
		reduceLayout.addView(reduceView);
		linearLayout.addView(radioGroup, LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		linearLayout.addView(qualityLayout, LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		linearLayout.addView(reduceLayout, LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(linearLayout, ScrollView.LayoutParams.MATCH_PARENT,
				ScrollView.LayoutParams.WRAP_CONTENT);
		return new AlertDialog.Builder(context).setTitle(R.string.reencode_image)
				.setView(scrollView).setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, this).create();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(EXTRA_QUALITY, qualityForm.getCurrentValue());
		outState.putInt(EXTRA_REDUCE, reduceForm.getCurrentValue());
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		String format = null;
		int id = radioGroup.getCheckedRadioButtonId();
		for (int i = 0; i < IDS.length; i++) {
			if (IDS[i] == id) {
				format = FORMATS[i];
				break;
			}
		}
		((AttachmentOptionsDialog) getParentFragment()).setReencoding(new GraphicsUtils
				.Reencoding(format, qualityForm.getCurrentValue(), reduceForm.getCurrentValue()));
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		boolean allowQuality = true;
		for (int i = 0; i < IDS.length; i++) {
			if (IDS[i] == checkedId) {
				allowQuality = GraphicsUtils.Reencoding.allowQuality(FORMATS[i]);
				break;
			}
		}
		qualityForm.getSeekBar().setEnabled(allowQuality);
	}
}
