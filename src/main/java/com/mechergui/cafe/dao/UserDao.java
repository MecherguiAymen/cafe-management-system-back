package com.mechergui.cafe.dao;

import com.mechergui.cafe.POJO.User;
import com.mechergui.cafe.wrapper.UserWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface UserDao extends JpaRepository<User,Integer> {
  User findByEmailId(@Param("email")String email);

  List<UserWrapper> getAllUsers();

  List<String> getAllAdmin();

  @Modifying
  @Transactional
  Integer updateStatus(@Param("status")String status,@Param("id")Integer id);

  User findByEmail(String email);
}
