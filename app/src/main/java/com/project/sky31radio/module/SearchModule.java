package com.project.sky31radio.module;

import com.project.sky31radio.ui.SearchActivity;
import com.project.sky31radio.ui.base.InjectableActivity;
import com.project.sky31radio.ui.fragment.SearchProgramListFragment;

import dagger.Module;

/**
 * Created by linroid on 1/22/15.
 */
@Module(
        complete = false,
        overrides = true,
        injects = {
                SearchActivity.class,
                SearchProgramListFragment.class
        }
)
public class SearchModule {

    InjectableActivity activity;

    public SearchModule(InjectableActivity activity) {
        this.activity = activity;
    }
}
