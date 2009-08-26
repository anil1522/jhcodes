package com.jhlee.vbudget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.jhlee.vbudget.CommandBar.OnCommandExecuteListener;
import com.jhlee.vbudget.camera.RRTakeReceiptActivity;

public class VisualBudget extends Activity implements OnCommandExecuteListener {
	private CommandBar mCmdBar;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.visual_budget);

		/* Command bar */
		mCmdBar = (CommandBar) findViewById(R.id.command_bar);
		mCmdBar.setOnCommandExecuteListener(this);
	}

	/*
	 * Command is clicked
	 */
	@Override
	public void onCommandExecute(int cmd) {
		switch (cmd) {
		case CommandBar.COMMAND_CAMERA: {
			Intent i = new Intent(this, RRTakeReceiptActivity.class);
			this.startActivity(i);
		}
			break;
		case CommandBar.COMMAND_SHOW_THE_PLAN:
			break;
		}
	}

}