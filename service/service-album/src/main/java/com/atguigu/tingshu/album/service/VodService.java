package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;

public interface VodService {

    /**
     * 获取流媒体信息
     * @param mediaFileId
     * @return
     */
    TrackMediaInfoVo getTrackMediaInfo(String mediaFileId);

    /**
     * 删除流媒体声音
     * @param mediaFileId
     */
    void removeTrack(String mediaFileId);

}
