package com.uni.wt.project.post.model.dao;

import com.uni.wt.project.post.model.dto.Post;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper {

    public int insertPost(Post post);

}