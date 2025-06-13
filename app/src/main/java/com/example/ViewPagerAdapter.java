package com.example;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.fragments.FragmentFocusMode;
import com.example.fragments.FragmentMenu;
import com.example.fragments.PlaceholderFragment;
import com.example.fragments.FragmentChatAI;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private boolean showMenuFragment = false;
    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0 && !showMenuFragment) {
            return new PlaceholderFragment();
        }
        switch (position) {
            case 0:
                return new FragmentMenu();
            case 1:
                return new FragmentFocusMode();
        }
            case 2:
                return new FragmentChatAI();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
        return 3;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // Buộc ViewPager làm mới Fragment khi notifyDataSetChanged được gọi
        return POSITION_NONE;
    }

    public void setShowMenuFragment() {
        showMenuFragment = true;
        notifyDataSetChanged();
    }

}
