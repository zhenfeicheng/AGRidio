package com.project.sky31radio.interfaces;

import java.io.File;

/**
 * Created  on 2018/2/2.
 */

public interface DownloadFileListener {
    void onFinish(boolean success, File file);

    void onProgress(int progress);
}
