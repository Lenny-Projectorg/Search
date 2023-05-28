package com.find.search.service;

import com.find.search.entity.Found;
import java.util.HashMap;
import java.util.List;

public interface FoundService {
    //查询所有失物招领
    List<Found> selectAllFound();
    //分页查询所有失物招领方法:两个参数：页码，每页条数
    HashMap<String, Object> findFoundByPage(Integer page, Integer pageRow);

    //查询指定用户发布的失物招领
    List<Found> selectFoundByUserId(Found found);

    //分页查询指定用户发布的失物招领
    HashMap<String,Object> findUserIdFoundByPage(Integer page, Integer pageRow, Found found);

    //添加失物招领
    HashMap<String,Object> insertFound(Found found);

    HashMap<String,Object> updateFound(Found found);
}
