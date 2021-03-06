package com.jhlee.vbudget.tags;

import junit.framework.Assert;
import android.database.Cursor;
import android.util.Log;

import com.jhlee.vbudget.db.RRDbAdapter;
import com.jhlee.vbudget.tags.RRTagsListView.RRTagDataProvider;

public class RRTagDataProviderFromDb implements RRTagDataProvider {
	private static final String TAG = "RRTagDataProviderFromDb";
	private static final int TAG_STRING_COL_INDEX = 1;
	private RRDbAdapter	mDbAdapter;
	private Cursor mCursor;
	private int	mActiveReceiptId = -1;

	/*
	 * CTOR
	 */
	public RRTagDataProviderFromDb(RRDbAdapter adapter) {
		mDbAdapter = adapter;
		refreshAllTags();
	}

	/*
	 * Refresh all tags from TAG SOURCE table
	 */
	private void refreshAllTags() {
		/* Before getting new data,
		 * first close cursor
		 */
		if(mCursor != null) {
			mCursor.close();
			mCursor = null;	
		}
		
		mCursor = mDbAdapter.queryAllTags();
	}
	
	public void setActiveReceiptId(int activeReceiptId) {
		mActiveReceiptId = activeReceiptId;
	}
	
	private boolean hasValidReceiptId() {
		return mActiveReceiptId != -1;
	}

	/*
	 * Add new tag and change its status
	 */
	public boolean addTag(String tag, boolean checked) {
		if(false == mDbAdapter.createTag(tag)) {
			Log.e(TAG, "Unable to add tag to DB:tagName=" + tag);
			return false;
		}
		
		/* Refresh all tags */
		refreshAllTags();
		
		/* Check tag */
		if(checked == true) {
			/*
			 * New tag can be created during entering expense data.
			 * Hence we check receipt id validity. 
			 */
			if(true == this.hasValidReceiptId()) {
				mDbAdapter.addTagToReceipt(mActiveReceiptId, tag);
			}
		}
		
		return true;
	}

	public void check(int index) {
		String tag = getTag(index);
		mDbAdapter.addTagToReceipt(mActiveReceiptId, tag);
	}

	public int getCount() {
		return mCursor.getCount();
	}

	public String getTag(int index) {
		mCursor.moveToPosition(index);
		return mCursor.getString(TAG_STRING_COL_INDEX);
	}

	public boolean isChecked(int index) {
		String tag = getTag(index);
		return mDbAdapter.doesReceiptHaveTag(mActiveReceiptId, tag);
	}

	public void uncheck(int index) {
		String tag = getTag(index);
		mDbAdapter.removeTagFromReceipt(mActiveReceiptId, tag);
	}

	/*
	 * Find tag in tag source.
	 */
	public int findTag(String tagName) {
		mCursor.moveToFirst();
		int index = 0;
		while(false == mCursor.isAfterLast()) {
			if(0 == tagName.compareToIgnoreCase(mCursor.getString(TAG_STRING_COL_INDEX)))
				return index;
					
			++index;
			mCursor.moveToNext();
		}
		return -1;
	}
}
