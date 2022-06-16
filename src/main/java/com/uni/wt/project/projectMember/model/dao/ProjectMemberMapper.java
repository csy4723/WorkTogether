package com.uni.wt.project.projectMember.model.dao;

import com.uni.wt.project.projectMember.model.dto.ProjectMember;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface ProjectMemberMapper {
    public int insertProjectMember(ProjectMember pjm);

    int insertBookmark(ProjectMember pjm);

    int removeBookmark(ProjectMember pjm);

    ArrayList<ProjectMember> selectProjectColor(int loginEmp);

    int setProjectColor(ProjectMember pjm);
}