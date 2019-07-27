package com.msw.modules.system.service.impl;

import com.msw.exception.EntityNotFoundException;
import com.msw.modules.monitor.service.RedisService;
import com.msw.modules.system.domain.vo.UserVo;
import com.msw.modules.system.repository.UserRepository;
import com.msw.modules.system.service.dto.UserQueryCriteria;
import com.msw.modules.system.service.mapper.UserMapper;
import com.msw.utils.PageUtil;
import com.msw.utils.QueryHelp;
import com.msw.utils.ValidationUtil;
import com.msw.modules.system.domain.User;
import com.msw.exception.EntityExistException;
import com.msw.modules.system.service.UserService;
import com.msw.modules.system.service.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author mashuangwei
 * @date 2018-11-23
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public Object queryAll(UserQueryCriteria criteria, Pageable pageable) {
        Page<User> page = userRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root,criteria,criteriaBuilder),pageable);
        return PageUtil.toPage(page.map(userMapper::toDto));
    }

    @Override
    public UserDTO findById(long id) {
        Optional<User> user = userRepository.findById(id);
        ValidationUtil.isNull(user,"User","id",id);
        return userMapper.toDto(user.get());
    }

    /**
     * 根据登录用户的ID查出该用户所在部门的所有用户
     * @param id
     * @return
     */
    @Override
    public List<UserVo> findUsersById(long id) {
        List<User> userList = userRepository.findUsersById(id);
        Iterator<User> iterator = userList.iterator();
        List<UserVo> userVoList = new ArrayList<>();
        while (iterator.hasNext()) {
            User user = iterator.next();
            UserVo userVo = new UserVo();
            userVo.setId(user.getId());
            userVo.setChina_name(user.getChinaName());
            userVo.setEmail(user.getEmail());
            userVo.setUsername(user.getUsername());
            userVo.setEnabled(user.getEnabled());
            userVo.setJob_id(user.getJob().getId());
            userVo.setDept_id(user.getDept_id());
            userVo.setDept(user.getDept().getName());

            userVoList.add(userVo);
        }

        return userVoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO create(User resources) {

        if(userRepository.findByUsername(resources.getUsername())!=null){
            throw new EntityExistException(User.class,"username",resources.getUsername());
        }

        if(userRepository.findByEmail(resources.getEmail())!=null){
            throw new EntityExistException(User.class,"email",resources.getEmail());
        }

        // 默认密码 123456，此密码是加密后的字符
        resources.setPassword("e10adc3949ba59abbe56e057f20f883e");
        resources.setAvatar("https://i.loli.net/2019/04/04/5ca5b971e1548.jpeg");
        return userMapper.toDto(userRepository.save(resources));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User resources) {
        Optional<User> userOptional = userRepository.findById(resources.getId());
        ValidationUtil.isNull(userOptional,"User","id",resources.getId());

        User user = userOptional.get();

        User user1 = userRepository.findByUsername(user.getUsername());
        User user2 = userRepository.findByEmail(user.getEmail());

        if(user1 !=null&&!user.getId().equals(user1.getId())){
            throw new EntityExistException(User.class,"username",resources.getUsername());
        }

        if(user2!=null&&!user.getId().equals(user2.getId())){
            throw new EntityExistException(User.class,"email",resources.getEmail());
        }

        // 如果用户的角色改变了，需要手动清理下缓存
        if (!resources.getRoles().equals(user.getRoles())) {
            String key = "role::loadPermissionByUser:" + user.getUsername();
            redisService.delete(key);
            key = "role::findByUsers_Id:" + user.getId();
            redisService.delete(key);
        }

        user.setUsername(resources.getUsername());
        user.setChinaName(resources.getChinaName());
        user.setEmail(resources.getEmail());
        user.setEnabled(resources.getEnabled());
        user.setRoles(resources.getRoles());
        user.setDept(resources.getDept());
        user.setJob(resources.getJob());
        user.setPhone(resources.getPhone());
        userRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDTO findByName(String userName) {
        User user = null;
        if(ValidationUtil.isEmail(userName)){
            user = userRepository.findByEmail(userName);
        } else {
            user = userRepository.findByUsername(userName);
        }
        if (user == null) {
            throw new EntityNotFoundException(User.class, "name", userName);
        } else {
            return userMapper.toDto(user);
        }
    }

    @Override
    public List<UserVo> findByUserName(String userName) {
        List<User> userList = userRepository.findAllByUsernameContaining(userName);
        List<UserVo> userVoList = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            UserVo userVo = new UserVo();
            userVo.setChina_name(userList.get(i).getChinaName());
            userVo.setId(userList.get(i).getId());
            userVo.setUsername(userList.get(i).getUsername());
            userVoList.add(userVo);
        }
        return userVoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePass(String username, String pass) {
        userRepository.updatePass(username,pass,new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String username, String url) {
        userRepository.updateAvatar(username,url);
    }

    @Override
    public void updateName(String username, String chinaName) {
        userRepository.updateName(username, chinaName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(String username, String email) {
        userRepository.updateEmail(username,email);
    }
}
