package com.project.sky31radio.module;



import com.project.sky31radio.ui.HomeActivity;
import com.project.sky31radio.ui.base.InjectableActivity;
import com.project.sky31radio.ui.fragment.AlbumListFragment;
import com.project.sky31radio.ui.fragment.AnchorListFragment;
import com.project.sky31radio.ui.fragment.PlayerFragment;
import com.project.sky31radio.ui.fragment.ProgramListFragment;

import dagger.Module;

@Module(
        complete = false,
        injects = {
                HomeActivity.class,
                ProgramListFragment.class,
                PlayerFragment.class,
                AlbumListFragment.class,
                AnchorListFragment.class,
                //ShareFragment.class
        }
)
public class HomeModule {
    InjectableActivity activity;

    public HomeModule() {
    }

    public HomeModule(InjectableActivity activity) {
        this.activity = activity;
    }

//    @Provides
//    @Singleton
//    public PostAdapter providePostAdapter() {
//        PostAdapter localPostAdapter = new PostAdapter(null);
//        return localPostAdapter;
//    }
}

/* Location:           /home/linroid/Tmp/Alwen/classes-dex2jar.jar
 * Qualified Name:     com.linroid.alwen.module.HomeModule
 * JD-Core Version:    0.6.2
 */