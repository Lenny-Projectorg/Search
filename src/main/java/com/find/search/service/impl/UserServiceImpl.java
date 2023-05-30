package com.find.search.service.impl;

import com.find.search.entity.User;
import com.find.search.mapper.UserMapper;
import com.find.search.service.UserService;
import com.find.search.utils.Email;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    Email email;

    /**
     * 用户登录
     * @param user
     * @return
     */
    @Override
    public HashMap<String, Object> selectLogin(User user, HttpSession session) {
        //创建HashMap返回值集合
        HashMap<String,Object> map=new HashMap<>();
        //判断邮箱是否已经注册
        User user1 = userMapper.selectByUserEmail(user.getUserEmail());
        if(user1==null){
            map.put("info","该邮箱未注册");
            return map;
        }

        //判断密码是否正确
        //获取对应用户的盐值
        String salt = user1.getSalt();
        //对密码进行加密比对
        String simpleHash = new SimpleHash("MD5", user.getUserPassword(),salt, 2).toString();

        User user2 = userMapper.selectLoginIndo(user.getUserEmail(), simpleHash);
        if(user2==null){
            map.put("info","密码错误");
            return map;
        }

        //用户通过登录验证
        map.put("info","登录成功");

        //设置用户登录Session
        session.setAttribute("uid",user2.getUserId());
        session.setAttribute("userEmail",user2.getUserEmail());

        return map;
    }

    /**
     * 用户注册方法
     * @param user
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//表示发生异常时  回滚事务
    public HashMap<String, Object> insertUser(User user) {
        //创建HashMap返回值集合
        HashMap<String,Object> map=new HashMap<>();

        //1、判断验证码是否正确
        String code = redisTemplate.opsForValue().get("code") + "";//从redis中取出验证码
        System.out.println(code);
        if (!user.getCheckCode().equals(code)){
            map.put("info","验证码输入错误");
            return map;
        }

        //2、验证码正确   进行注册
        //密码加密
        String password = user.getUserPassword();
        //设置密码MD5加密
        String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
        System.out.println("盐值:"+salt);
        String simpleHash = new SimpleHash("MD5", password,salt, 2).toString();
        System.out.println(simpleHash);

        //补全user信息
        user.setUserPassword(simpleHash);
        user.setSalt(salt);
        user.setPower(0);

        //密码解密
//        String encode = encoder.encode(user.getUserPassword());
////        System.out.println(encode);
//        user.setUserPassword(encode);

        int i = userMapper.insertUser(user);
        if(i>0){
            map.put("info","注册成功");
        }else{
            map.put("info","注册失败");
        }

        return map;
    }

    /**
     * 向注册邮箱发送验证码
     * @param userEmail
     * @return
     */
    @Override
    public HashMap<String, Object> sendCode(String userEmail) {
        //创建HashMap返回值集合
        HashMap<String,Object> map=new HashMap<>();
        //判断邮箱是否注册过
        User user = userMapper.selectByUserEmail(userEmail);
        if(user!=null){
            map.put("info","该邮箱已经注册");
            return map;
        }

        //生成验证码
        String code="";
        for (int i=1;i<=6;i++){
            code+=new Random().nextInt(10);
        }
        //发送验证码
        boolean result = email.sendEmail(userEmail, "验证码", "你的验证码是:" + code);

        //将验证码放入redis中存储30秒
        redisTemplate.opsForValue().set("code",code,30, TimeUnit.SECONDS);

        if(result){
            map.put("info","发送成功");
        }else{
            map.put("info","发送失败");
        }

        return map;
    }

    /**
     * 根据用户邮箱获取用户id
     * @return
     */
    @Override
    public HashMap<String,Object> selectByUserEmail(String userEmail){
        HashMap<String, Object> map = new HashMap<>();
        User user = userMapper.selectByUserEmail(userEmail);
        if(user.getUserId()!=null){
            map.put("info",user.getUserId());
        }else {
            map.put("info","未查询到该用户");
        }
        return map;
    }

    /**
     * 根据邮箱查询指定用户
     * @param userEmail
     * @return
     */
    @Override
    public HashMap<String, Object> selectUser(String userEmail){
        HashMap<String, Object> map = new HashMap<>();
        User user = userMapper.selectByUserEmail(userEmail);
        if(user.getUserId()==null){
            map.put("failed",null);
        }else {
            map.put("success",user);
        }
        return map;
    }

    /**
     * 修改用户信息
     * @param user
     * @return
     */
    @Override
    public HashMap<String, Object> editUserInfo(User user) {
        HashMap<String, Object> map = new HashMap<>();
        Integer i = userMapper.editUserInfo(user);
        if(i>0){
            map.put("info","修改成功");
        }else {
            map.put("info","修改失败");
        }
        return map;
    }

    /**
     * 修改用户密码
     * @param user
     * @return
     */
    @Override
    public HashMap<String, Object> editUserPassword(User user) {
        HashMap<String, Object> map = new HashMap<>();

        //判断邮箱是否正确
        User user1 = userMapper.selectByUserEmail(user.getUserEmail());
        System.out.println("user1:"+user1);
        if(user1.getUserId()==null){
            map.put("info","邮箱不正确");
            return map;
        }
        //判断验证码是否正确
        String code = redisTemplate.opsForValue().get("code") + "";//从redis中取出验证码
//        System.out.println(code);
        if (!user.getCheckCode().equals(code)){
            map.put("info","验证码输入错误");
            return map;
        }

        //获取用户盐值
        String salt = user1.getSalt();
        //对新密码进行加密
        String simpleHash = new SimpleHash("MD5", user.getUserPassword(),salt, 2).toString();
//        System.out.println(simpleHash);
        //对用户密码重新赋值
        user.setUserPassword(simpleHash);
        //进行修改
        Integer i = userMapper.editUserPassword(user);
        if(i>0){
            map.put("info","修改成功");
        }else {
            map.put("info","修改失败");
        }

        return map;
    }

    /**
     * 发送修改密码的验证码
     * @param userEmail
     * @return
     */
    @Override
    public HashMap<String, Object> sendPasswordCode(String userEmail) {
        //创建HashMap返回值集合
        HashMap<String,Object> map=new HashMap<>();
        //判断邮箱是否注册过
        User user = userMapper.selectByUserEmail(userEmail);
        if(user==null){
            map.put("info","未查询到该邮箱");
            return map;
        }

        //生成验证码
        String code="";
        for (int i=1;i<=6;i++){
            code+=new Random().nextInt(10);
        }
        //发送验证码
        boolean result = email.sendEmail(userEmail, "验证码", "你的验证码是:" + code);

        //将验证码放入redis中存储30秒
        redisTemplate.opsForValue().set("code",code,30, TimeUnit.SECONDS);

        if(result){
            map.put("info","发送成功");
        }else{
            map.put("info","发送失败");
        }

        return map;
    }
}
