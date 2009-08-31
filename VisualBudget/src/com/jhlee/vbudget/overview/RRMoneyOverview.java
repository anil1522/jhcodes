package com.jhlee.vbudget.overview;

import java.util.Iterator;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jhlee.vbudget.R;
import com.jhlee.vbudget.RRBudgetContent;
import com.jhlee.vbudget.collect.RRTakeReceiptActivity;
import com.jhlee.vbudget.collect.RRTransactionEditDialog;
import com.jhlee.vbudget.db.RRDbAdapter;
import com.jhlee.vbudget.util.RRUtil;

public class RRMoneyOverview extends FrameLayout implements RRBudgetContent {

	private static final String TAG = "RRMoneyOverview";

	private RRDbAdapter mDbAdapter;
	private TextView mYearMonthTextView;
	private TextView mBudgetBalanceTextView;
	private TextView mBudgetAmountTextView;
	private ProgressBar mBudgetProgress;
	private ListView mBudgetList;
	private ImageButton mCameraButton;
	private ImageButton mExpenseButton;
	private Handler mHandler = new Handler();
	private Runnable mDataRefreshTask = null;

	public RRMoneyOverview(Context context) {
		this(context, null);
	}

	public RRMoneyOverview(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RRMoneyOverview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public void initialize(RRDbAdapter dbAdapter) {
		mDbAdapter = dbAdapter;
		RRUtil.createViewsFromLayout(this.getContext(), R.layout.overview_main,
				this);

		mYearMonthTextView = (TextView) findViewById(R.id.budget_year_month);
		mBudgetBalanceTextView = (TextView) findViewById(R.id.budget_balance);
		mBudgetAmountTextView = (TextView) findViewById(R.id.budget_total);
		mBudgetProgress = (ProgressBar) findViewById(R.id.budget_progress_bar);
		mBudgetList = (ListView) findViewById(R.id.budget_list);
		mCameraButton = (ImageButton) findViewById(R.id.camera_button);
		mExpenseButton = (ImageButton) findViewById(R.id.add_expense_button);

		mCameraButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				/* Launch capture activity */
				Context ctx = RRMoneyOverview.this.getContext();
				Intent i = new Intent(ctx, RRTakeReceiptActivity.class);
				ctx.startActivity(i);
			}
		});

		/*
		 * Manual budget insert
		 */
		mExpenseButton.setOnClickListener(new OnClickListener() {

			/* Transaction button is clicked */
			@Override
			public void onClick(View v) {
				final RRTransactionEditDialog dlg = new RRTransactionEditDialog(
						RRMoneyOverview.this.getContext());
				dlg.initialize(mDbAdapter);

				dlg
						.setOnDismissListener(new DialogInterface.OnDismissListener() {
							@Override
							public void onDismiss(DialogInterface dialog) {
								/*
								 * We always refresh budget amount at here.
								 */
								RRMoneyOverview.this.requestDataRefresh();

								/* Dialog is closed */
								if (dlg.isCanceled())
									return;

								long transId = mDbAdapter.newTransaction();
								if (-1 == transId) {
									Log.e(TAG,
											"Unable to create new transaction");
									return;
								}

								/* Read-only tag list */
								Iterator<String> it = dlg.getTaglistIterator();
								while (it.hasNext()) {
									mDbAdapter.addTagToReceipt(transId, it
											.next());
								}
								long money = dlg.getExpenseAmount();
								mDbAdapter.updateExpenseAmount(transId,
										(int) money / 100, (int) money % 100);

								/* Apply budget field */
								long budgetId = dlg.getSelectedBudgetId();
								if (-1 == budgetId)
									return;

								mDbAdapter.makeTransactionFromBudget(transId,
										money, budgetId);
							}
						});

				dlg.show();
			}
		});
	}

	@Override
	public void createMenu(Menu menu, MenuInflater inflater) {
		menu.add(0, 0, 0, "About");
	}
	/*
	 * Menu item is selected
	 */
	@Override
	public void onMenuItemSelected(MenuItem mi) {

	}

	/*
	 * Request data refresh
	 */
	private void requestDataRefresh() {
		if (mDataRefreshTask == null) {
			mDataRefreshTask = new Runnable() {
				@Override
				public void run() {
					RRMoneyOverview.this.refreshContent();
				}
			};
		}
		if (mDataRefreshTask != null) {
			mHandler.removeCallbacks(mDataRefreshTask);
			mHandler.post(mDataRefreshTask);
		}
	}

	private class RRSimpleBudgetAdapter extends BaseAdapter {
		private Cursor mCursor;

		public RRSimpleBudgetAdapter() {
			mCursor = mDbAdapter.queryCurrentMonthBudgetItems();
		}

		@Override
		public int getCount() {
			return mCursor.getCount();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			mCursor.moveToPosition(position);

			convertView = RRUtil.createViewsFromLayout(RRMoneyOverview.this
					.getContext(), R.layout.overview_simple_budget_item, null);
			TextView budgetNameView = (TextView) convertView
					.findViewById(R.id.budget_name);
			TextView budgetGuideView = (TextView) convertView
					.findViewById(R.id.budget_guide);
			TextView budgetBalanceView = (TextView) convertView
					.findViewById(R.id.budget_balance);

			String budgetName = mCursor.getString(RRDbAdapter.COL_BUDGET_NAME);
			long balance = mCursor.getLong(RRDbAdapter.COL_BUDGET_BALANCE);
			long total = mCursor.getLong(RRDbAdapter.COL_BUDGET_AMOUNT);
			if (balance <= 0) {
				/* OVER */
				budgetGuideView.setText("OVER");
				budgetBalanceView.setTextColor(Color.rgb(0xc7, 0x35, 0x1d));
			} else {
				long balanceRatio = balance * 100 / total;
				if (balanceRatio < 15) {
					/* WARNING */
					budgetGuideView.setText("WARNING");
				} else {
					budgetGuideView.setText("");
				}
			}

			budgetNameView.setText(budgetName);
			budgetBalanceView.setText(RRUtil.formatMoney(balance, true));
			return convertView;
		}

	}

	/*
	 * Refresh content
	 */
	@Override
	public void refreshContent() {
		/*
		 * Set year month
		 */
		String yearMonthStr = RRUtil.getCurrentYearMonthString();
		mYearMonthTextView.setText(yearMonthStr);

		Cursor c = mDbAdapter.queryCurrentMonthBudget();
		if (c != null) {
			long balance = 0;
			long total = 0;
			if (c.getCount() >= 1) {
				balance = c.getLong(RRDbAdapter.COL_BUDGET_BALANCE);
				total = c.getLong(RRDbAdapter.COL_BUDGET_AMOUNT);
			}
			c.close();

			mBudgetBalanceTextView.setText(RRUtil.formatMoney(balance, true));
			mBudgetAmountTextView.setText(RRUtil.formatMoney(total, true));
			mBudgetAmountTextView.requestLayout();

			mBudgetProgress.setMax((int) total);
			mBudgetProgress.setProgress((int) balance);
		}

		mBudgetList.setAdapter(new RRSimpleBudgetAdapter());

		requestLayout();

	}

}
