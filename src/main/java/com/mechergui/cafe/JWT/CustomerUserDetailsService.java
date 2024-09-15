package com.mechergui.cafe.JWT;

import com.mechergui.cafe.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    @Autowired
    UserDao userDao;

    private com.mechergui.cafe.POJO.User userDetail;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       userDetail = userDao.
               findByEmailId(email);

       if (!Objects.isNull(userDetail)) return new User(userDetail.getEmail(),userDetail.getPassword(),new ArrayList<>());
       else throw new UsernameNotFoundException("user not found");
    }

    public com.mechergui.cafe.POJO.User getUserDetail(){
        return userDetail;
    }

}
