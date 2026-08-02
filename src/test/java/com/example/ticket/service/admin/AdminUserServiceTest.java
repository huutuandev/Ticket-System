package com.example.ticket.service.admin;

import com.example.ticket.dto.response.UserDto;
import com.example.ticket.entity.Role;
import com.example.ticket.entity.User;
import com.example.ticket.repository.RoleRepository;
import com.example.ticket.repository.UserRepository;
import com.example.ticket.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        // Role không có @Builder nên dùng constructor/setter
        role = new Role();
        role.setId(1L);
        role.setName("USER");

        // User có @Builder nên dùng builder
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build();
    }

    // Kiểm tra lấy danh sách user thành công, trả về đúng số lượng và dữ liệu
    @Test
    void getAllUsers_success() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        List<UserDto> result = adminUserService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        assertTrue(result.get(0).getRoles().contains("USER"));
        verify(userRepository, times(1)).findAll();
    }

    // Kiểm tra xoá user thành công khi user tồn tại
    @Test
    void deleteUser_success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // Act
        adminUserService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).existsById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    // Kiểm tra xoá user thất bại khi user không tồn tại, ném ra RuntimeException
    @Test
    void deleteUser_userNotFound() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminUserService.deleteUser(1L));
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).deleteById(anyLong());
    }

    // Kiểm tra cập nhật role cho user thành công, trả về DTO với role mới
    @Test
    void updateUserRole_success() {
        // Arrange
        // Role không có @Builder nên dùng constructor/setter
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserDto result = adminUserService.updateUserRole(1L, "admin");

        // Assert
        assertNotNull(result);
        assertTrue(result.getRoles().contains("ADMIN"));
        assertEquals(1, result.getRoles().size());
        verify(userRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).findByName("ADMIN");
        verify(userRepository, times(1)).save(user);
    }

    // Kiểm tra cập nhật role thất bại khi user không tồn tại
    @Test
    void updateUserRole_userNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminUserService.updateUserRole(1L, "admin"));
        assertEquals("User not found", exception.getMessage());
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // Kiểm tra cập nhật role thất bại khi role không tồn tại
    @Test
    void updateUserRole_roleNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminUserService.updateUserRole(1L, "admin"));
        assertEquals("Role not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
