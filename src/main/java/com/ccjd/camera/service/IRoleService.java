package com.ccjd.camera.service;

import com.ccjd.camera.storager.dao.dto.Role;

import java.util.List;

public interface IRoleService {

    Role getRoleById(int id);

    int add(Role role);

    int delete(int id);

    List<Role> getAll();

    int update(Role role);
}
