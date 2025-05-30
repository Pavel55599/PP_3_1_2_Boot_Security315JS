package ru.kata.spring.boot_security.demo.service;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repositories.UserRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;


    @Lazy
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;

    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);

    }


    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User '%s' not found ", username));
        }
        user.getRoles().size(); // Это "трюк" для загрузки ленивой коллекции
        return user;

    }


    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void save(User user) {
        if (user.getPassword() == null || !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);


    }

    @Override
    @Transactional
    public User saveUserWithRoles(User user, Set<Long> roleIds) {
        if (user.getPassword() == null || !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        user.setEnabled(user.isEnabled());

        // Используем новый метод из RoleService, логика создания списка осталась в RoleService, здесь используем только метод getRolesByIds
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = roleService.getRolesByIds(roleIds);
            user.setRoles(roles);
        }

        return userRepository.save(user);//сразу вернул
    }


    @Override
    public User findById(Long id) {
        return userRepository.findById(id);

    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();

    }

    @Override
    @Transactional
    public User updateUserWithRoles(Long id, User updatedUser, List<Long> roleIds) {
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }

        BeanUtils.copyProperties(updatedUser, existingUser, "id", "password", "roles");

        if (updatedUser.getPassword() != null
                && !updatedUser.getPassword().isEmpty()
                && !updatedUser.getPassword().equals(existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        // Используем новый метод из RoleService, логика создания списка осталась в RoleService, здесь используем только метод getRolesByIds
        if (roleIds != null) {
            existingUser.setRoles(roleService.getRolesByIds(new HashSet<>(roleIds)));
        }
        return userRepository.save(existingUser);//сразу вернул
    }


    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.delete(id);

    }
}



