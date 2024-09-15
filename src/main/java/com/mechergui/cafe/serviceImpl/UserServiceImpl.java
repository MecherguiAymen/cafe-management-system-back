package com.mechergui.cafe.serviceImpl;

import com.google.common.base.Strings;
import com.mechergui.cafe.JWT.CustomerUserDetailsService;
import com.mechergui.cafe.JWT.JwtFilter;
import com.mechergui.cafe.JWT.JwtUtils;
import com.mechergui.cafe.POJO.User;
import com.mechergui.cafe.constants.CafeConstants;
import com.mechergui.cafe.dao.UserDao;
import com.mechergui.cafe.service.UserService;
import com.mechergui.cafe.utils.CafeUtils;
import com.mechergui.cafe.utils.EmailUtils;
import com.mechergui.cafe.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    JwtFilter jwtFilter;

    @Autowired
    EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
      log.info("Inside sign up {}",requestMap);
      try {
      if (validateSignUpMap(requestMap)){
          User user=userDao.findByEmailId(requestMap.get("email"));
         if (Objects.isNull(user))
         {
             userDao.save(getUserFromMap(requestMap));
             return CafeUtils.getResponseEntity("Successfully registred", HttpStatus.OK);
         }
      else
         {
             return CafeUtils.getResponseEntity("Email already exist", HttpStatus.BAD_REQUEST);
         }

      }else {
          return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
      }}catch (Exception e){
          e.printStackTrace();
      }
      return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("inside login {}", requestMap);
        try {
            Authentication authentication=authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"),requestMap.get("password"))
            );
            if (authentication.isAuthenticated()){
                if (customerUserDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    return new ResponseEntity<String>("{\"token\":\""+jwtUtils.generateToken(customerUserDetailsService.getUserDetail().getEmail(),
                            customerUserDetailsService.getUserDetail().getRole())+"\"}",HttpStatus.OK);
                }else {
                     return new ResponseEntity<String>("{\"message\":\""+"wait for admin approval"+"\"}", HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception ex){
            log.error("{}",ex);
        }
        return new ResponseEntity<String>("{\"message\":\""+"bad credentials"+"\"}", HttpStatus.BAD_REQUEST);

    }





    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
          if (jwtFilter.isAdmin()){
              return new ResponseEntity<>(userDao.getAllUsers(),HttpStatus.OK);
          }else {
              return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
          }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> upadate(Map<String, String> requestMap) {
      try {
          if (jwtFilter.isAdmin()){
             Optional<User> optionalUser= userDao.findById(Integer.parseInt(requestMap.get("id")));
           if (!optionalUser.isEmpty()){
               userDao.updateStatus(requestMap.get("status"),Integer.parseInt(requestMap.get("id")));
               sendMailToAllAdmins(requestMap.get("status"),optionalUser.get().getEmail(),userDao.getAllAdmin());
               return CafeUtils.getResponseEntity("User Status updated Successfully",HttpStatus.OK);
           }else {
               CafeUtils.getResponseEntity("User id doesn't exist",HttpStatus.OK);
           }
          }else {
              return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS,HttpStatus.UNAUTHORIZED);
          }
         }catch (Exception ex){
          ex.printStackTrace();
      }
      return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }



    private void sendMailToAllAdmins(String status, String user, List<String> allAdmin) {
          allAdmin.remove(jwtFilter.getCurrentUser());
          if (status!=null && status.equalsIgnoreCase("true")){
             emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(),"Account approved","USER:-"+user+" \n is approved by \nADMIN:-"+jwtFilter.getCurrentUser(),allAdmin);
          }else {
              emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(),"Account disabled","USER:-"+user+" \n is disabled by \nADMIN:-"+jwtFilter.getCurrentUser(),allAdmin);

          }
    }

    private boolean validateSignUpMap(Map<String,String> requestMap){
        return requestMap.containsKey("name")
                && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email")
                && requestMap.containsKey("password");
    }

    private User getUserFromMap(Map<String,String> requestMap){
        User user=new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return CafeUtils.getResponseEntity("true",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
          User userObj=userDao.findByEmail(jwtFilter.getCurrentUser());
          if (!userObj.equals(null)){
             if (userObj.getPassword().equals(requestMap.get("oldPassword"))){
                 userObj.setPassword(requestMap.get("newPassword"));
                 userDao.save(userObj);
                 return CafeUtils.getResponseEntity("Password updated successfully",HttpStatus.OK);
             }
              return CafeUtils.getResponseEntity("Incorrect old password",HttpStatus.BAD_REQUEST);

          }
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User user=userDao.findByEmail(requestMap.get("email"));
            if (!Objects.isNull(user) && !Strings.isNullOrEmpty(user.getEmail()))
                emailUtils.forgotMail(user.getEmail(),"Credentials by cafe management system",user.getPassword() );
            return CafeUtils.getResponseEntity("check your email for credentials",HttpStatus.OK);


        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
