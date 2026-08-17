package com.bor.eboard.identity.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.constants.AppConstants;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.config.security.JwtService;
import com.bor.eboard.identity.dto.LoginRequest;
import com.bor.eboard.identity.dto.LoginResponse;
import com.bor.eboard.identity.dto.ForgotPasswordRequest;
import com.bor.eboard.identity.dto.OtpChallengeResponse;
import com.bor.eboard.identity.dto.OtpResendRequest;
import com.bor.eboard.identity.dto.OtpVerifyRequest;
import com.bor.eboard.identity.dto.PasswordResetTokenResponse;
import com.bor.eboard.identity.dto.PublicResetPasswordRequest;
import com.bor.eboard.identity.dto.RefreshTokenRequest;
import com.bor.eboard.identity.dto.UserInfo;
import com.bor.eboard.identity.entity.Department;
import com.bor.eboard.identity.entity.Designation;
import com.bor.eboard.identity.entity.Permission;
import com.bor.eboard.identity.entity.RefreshToken;
import com.bor.eboard.identity.entity.Role;
import com.bor.eboard.identity.entity.RolePermission;
import com.bor.eboard.identity.entity.Section;
import com.bor.eboard.identity.entity.User;
import com.bor.eboard.identity.entity.UserRole;
import com.bor.eboard.identity.entity.AuthenticationOtp;
import com.bor.eboard.identity.repository.AuthenticationOtpRepository;
import com.bor.eboard.identity.repository.DepartmentRepository;
import com.bor.eboard.identity.repository.DesignationRepository;
import com.bor.eboard.identity.repository.PermissionRepository;
import com.bor.eboard.identity.repository.RefreshTokenRepository;
import com.bor.eboard.identity.repository.RolePermissionRepository;
import com.bor.eboard.identity.repository.RoleRepository;
import com.bor.eboard.identity.repository.SectionRepository;
import com.bor.eboard.identity.repository.UserRoleRepository;
import com.bor.eboard.identity.repository.UserRepository;
import com.bor.eboard.identity.service.AuthService;
import com.bor.eboard.identity.service.CaptchaService;
import com.bor.eboard.identity.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Authentication: login with failed-attempt lockout, rotating refresh tokens
 * stored hashed, logout revocation, and current-user resolution.
 * Relationships (roles, permissions, org names) are assembled manually via
 * repository calls - no JPA associations (07_CLAUDE.md section 4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DepartmentRepository departmentRepository;
    private final SectionRepository sectionRepository;
    private final DesignationRepository designationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final CaptchaService captchaService;
    private final OtpService otpService;
    private final AuthenticationOtpRepository authenticationOtpRepository;

    @Value("${security.login.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.login.lock-duration-minutes:30}")
    private long lockDurationMinutes;

    @Value("${security.password-reset.password-expiry-days:0}")
    private long passwordExpiryDays;

    @Value("${security.password-reset.minimum-length:8}")
    private int passwordMinimumLength;

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public OtpChallengeResponse login(LoginRequest request) {
        if (!captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha())) {
            auditService.recordAuth(null, request.getUsername(),
                    AppConstants.AUDIT_ACTION_CAPTCHA_FAILED, false, "Invalid or expired CAPTCHA");
            throw new UnauthorizedException("Invalid or expired CAPTCHA. Please try the new CAPTCHA.");
        }
        auditService.recordAuth(null, request.getUsername(),
                AppConstants.AUDIT_ACTION_CAPTCHA_VALIDATED, true, null);
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> {
                    auditService.recordAuth(null, request.getUsername(),
                            AppConstants.AUDIT_ACTION_LOGIN_FAILED, false, "Unknown username");
                    return new UnauthorizedException("Invalid username or password");
                });

        if (Boolean.TRUE.equals(user.getAccountLocked()) && user.getAccountLockedUntil() != null
                && user.getAccountLockedUntil().isBefore(LocalDateTime.now())) {
            user.setAccountLocked(Boolean.FALSE);
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_LOGIN_FAILED, false, "Account locked");
            throw new UnauthorizedException(
                    "Account is locked due to repeated failed login attempts. Contact the administrator.");
        }

        if (!AppConstants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_LOGIN_FAILED, false,
                    "Account status: " + user.getStatus());
            throw new UnauthorizedException(
                    "Account is not active. Contact the administrator.");
        }

        validateOrganizationAndRoles(user);

        if (passwordExpiryDays > 0 && user.getPasswordChangedAt() != null
                && user.getPasswordChangedAt().plusDays(passwordExpiryDays).isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Password has expired. Use Forgot Password to set a new password.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = (user.getFailedLoginAttempts() == null
                    ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setAccountLocked(Boolean.TRUE);
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                auditService.recordAuth(user.getId(), user.getUsername(),
                        AppConstants.AUDIT_ACTION_ACCOUNT_LOCKED, false, "Maximum login attempts exceeded");
            }
            userRepository.save(user);
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_LOGIN_FAILED, false,
                    "Wrong password (attempt " + attempts + ")");
            throw new UnauthorizedException("Invalid username or password");
        }

        if (user.getMobile() == null || !user.getMobile().matches("^[0-9]{10}$")) {
            throw new UnauthorizedException("No valid mobile number is registered. Contact the administrator.");
        }
        return otpService.create(user, OtpService.LOGIN);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResponse verifyLoginOtp(OtpVerifyRequest request) {
        User user = userForChallenge(request.challengeId(), OtpService.LOGIN);
        validateLoginEligible(user);
        AuthenticationOtp challenge = otpService.verify(
                request.challengeId(), request.enteredOtp(), request.encryptedOtp(),
                OtpService.LOGIN, user);
        challenge.setConsumed(Boolean.TRUE);
        authenticationOtpRepository.save(challenge);
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return issueLoginResponse(user);
    }

    @Override
    @Transactional
    public OtpChallengeResponse resendLoginOtp(OtpResendRequest request) {
        User user = userForChallenge(request.challengeId(), OtpService.LOGIN);
        validateLoginEligible(user);
        return otpService.resend(request.challengeId(), OtpService.LOGIN, user);
    }

    @Override
    @Transactional
    public OtpChallengeResponse forgotPassword(ForgotPasswordRequest request) {
        auditService.recordAuth(null, request.username(),
                AppConstants.AUDIT_ACTION_PASSWORD_RESET_REQUESTED, true, null);
        User user = userRepository.findByUsernameAndDeletedFalse(request.username()).orElse(null);
        if (user == null || !AppConstants.USER_STATUS_ACTIVE.equals(user.getStatus())
                || Boolean.TRUE.equals(user.getAccountLocked())
                || user.getMobile() == null || !user.getMobile().matches("^[0-9]{10}$")) {
            return decoyChallenge();
        }
        try {
            return genericForgotResponse(otpService.create(user, OtpService.PASSWORD_RESET));
        } catch (RuntimeException ex) {
            log.warn("Password reset OTP request could not be completed for user {}", user.getId());
            return decoyChallenge();
        }
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public PasswordResetTokenResponse verifyPasswordResetOtp(OtpVerifyRequest request) {
        User user = userForChallenge(request.challengeId(), OtpService.PASSWORD_RESET);
        AuthenticationOtp challenge = otpService.verify(
                request.challengeId(), request.enteredOtp(), request.encryptedOtp(),
                OtpService.PASSWORD_RESET, user);
        return new PasswordResetTokenResponse(otpService.issueResetToken(challenge),
                otpService.getResetTokenExpirySeconds());
    }

    @Override
    @Transactional
    public OtpChallengeResponse resendPasswordResetOtp(OtpResendRequest request) {
        try {
            User user = userForChallenge(request.challengeId(), OtpService.PASSWORD_RESET);
            return genericForgotResponse(
                    otpService.resend(request.challengeId(), OtpService.PASSWORD_RESET, user));
        } catch (RuntimeException ex) {
            return decoyChallenge();
        }
    }

    @Override
    @Transactional
    public void resetForgottenPassword(PublicResetPasswordRequest request) {
        AuthenticationOtp challenge;
        try {
            challenge = otpService.findResetToken(request.resetToken());
        } catch (RuntimeException ex) {
            auditService.recordAuth(null, null, AppConstants.AUDIT_ACTION_PASSWORD_RESET_FAILED,
                    false, "Invalid reset session");
            throw ex;
        }
        User user = userRepository.findByIdAndDeletedFalse(challenge.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Password reset session is invalid"));
        if (request.newPassword().length() < passwordMinimumLength) {
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_PASSWORD_RESET_FAILED, false, "Password policy failure");
            throw new UnauthorizedException("Password must contain at least " + passwordMinimumLength + " characters");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            auditService.recordAuth(user.getId(), user.getUsername(),
                    AppConstants.AUDIT_ACTION_PASSWORD_RESET_FAILED, false, "Password reuse attempted");
            throw new UnauthorizedException("New password cannot be the same as the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(Boolean.FALSE);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now());
        challenge.setConsumed(Boolean.TRUE);
        authenticationOtpRepository.save(challenge);
        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_PASSWORD_RESET, true, null);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashRefreshToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(Boolean.TRUE);
            stored.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Refresh token expired. Please login again.");
        }

        User user = userRepository.findByIdAndDeletedFalse(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        // Token refresh must validate the user is still active (09_SECURITY.md 10).
        if (!AppConstants.USER_STATUS_ACTIVE.equals(user.getStatus())
                || Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new UnauthorizedException("Account is not active");
        }

        // Rotate: revoke the presented token and issue a new one.
        stored.setRevoked(Boolean.TRUE);
        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        List<String> roles = resolveActiveRoleCodes(user.getId());
        List<String> permissions = resolvePermissionCodes(user.getId());

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions);
        String newRefreshToken = issueRefreshToken(user.getId());

        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_TOKEN_REFRESH, true, null);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .user(buildUserInfo(user, roles, permissions))
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
        User user = userRepository.findByIdAndDeletedFalse(userId).orElse(null);
        auditService.recordAuth(userId, user != null ? user.getUsername() : null,
                AppConstants.AUDIT_ACTION_LOGOUT, true, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo currentUser(UUID userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<String> roles = resolveActiveRoleCodes(userId);
        List<String> permissions = resolvePermissionCodes(userId);
        return buildUserInfo(user, roles, permissions);
    }

    private User userForChallenge(UUID challengeId, String purpose) {
        AuthenticationOtp challenge = authenticationOtpRepository.findByIdAndPurpose(challengeId, purpose)
                .orElseThrow(() -> new UnauthorizedException("OTP challenge is invalid or expired"));
        return userRepository.findByIdAndDeletedFalse(challenge.getUserId())
                .orElseThrow(() -> new UnauthorizedException("OTP challenge is invalid or expired"));
    }

    private void validateLoginEligible(User user) {
        if (!AppConstants.USER_STATUS_ACTIVE.equals(user.getStatus())
                || Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new UnauthorizedException("Account is not active");
        }
        validateOrganizationAndRoles(user);
    }

    private void validateOrganizationAndRoles(User user) {
        if (user.getDepartmentId() != null && departmentRepository
                .findByIdAndDeletedFalse(user.getDepartmentId())
                .filter(d -> Boolean.TRUE.equals(d.getActive())).isEmpty()) {
            throw new UnauthorizedException("Assigned department is inactive. Contact the administrator.");
        }
        if (user.getSectionId() != null && sectionRepository
                .findByIdAndDeletedFalse(user.getSectionId())
                .filter(s -> Boolean.TRUE.equals(s.getActive())).isEmpty()) {
            throw new UnauthorizedException("Assigned section is inactive. Contact the administrator.");
        }
        if (resolveActiveRoleCodes(user.getId()).isEmpty()) {
            throw new UnauthorizedException("No active role is assigned. Contact the administrator.");
        }
    }

    private LoginResponse issueLoginResponse(User user) {
        List<String> roles = resolveActiveRoleCodes(user.getId());
        List<String> permissions = resolvePermissionCodes(user.getId());
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions);
        String refreshTokenValue = issueRefreshToken(user.getId());
        auditService.recordAuth(user.getId(), user.getUsername(),
                AppConstants.AUDIT_ACTION_LOGIN, true, null);
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .user(buildUserInfo(user, roles, permissions))
                .build();
    }

    private OtpChallengeResponse decoyChallenge() {
        return new OtpChallengeResponse(UUID.randomUUID(), otpService.createDecoyEncryptedOtp(),
                null, otpService.getExpirySeconds(), otpService.getMaxResends());
    }

    private OtpChallengeResponse genericForgotResponse(OtpChallengeResponse response) {
        return new OtpChallengeResponse(response.challengeId(), response.encryptedOtp(), null,
                response.expiresIn(), response.resendsRemaining());
    }

    // ------------------------------------------------------------------
    // Manual relationship assembly
    // ------------------------------------------------------------------

    private List<String> resolveActiveRoleCodes(UUID userId) {
        List<UUID> roleIds = activeRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByIdInAndDeletedFalse(roleIds).stream()
                .filter(role -> Boolean.TRUE.equals(role.getActive()))
                .map(Role::getCode)
                .distinct()
                .toList();
    }

    private List<String> resolvePermissionCodes(UUID userId) {
        List<UUID> roleIds = activeRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<UUID> permissionIds = rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findByIdInAndDeletedFalse(permissionIds).stream()
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    private List<UUID> activeRoleIds(UUID userId) {
        LocalDate today = LocalDate.now();
        return userRoleRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(ur -> ur.getEffectiveFrom() == null
                        || !ur.getEffectiveFrom().isAfter(today))
                .filter(ur -> ur.getEffectiveTo() == null
                        || !ur.getEffectiveTo().isBefore(today))
                .map(UserRole::getRoleId)
                .distinct()
                .toList();
    }

    private String issueRefreshToken(UUID userId) {
        String value = jwtService.generateRefreshTokenValue();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(jwtService.hashRefreshToken(value));
        refreshToken.setExpiresAt(
                LocalDateTime.now().plusDays(jwtService.getRefreshTokenExpiryDays()));
        refreshToken.setRevoked(Boolean.FALSE);
        refreshTokenRepository.save(refreshToken);
        return value;
    }

    private UserInfo buildUserInfo(User user, List<String> roles, List<String> permissions) {
        String departmentName = null;
        String sectionName = null;
        String designationName = null;
        if (user.getDepartmentId() != null) {
            departmentName = departmentRepository.findByIdAndDeletedFalse(user.getDepartmentId())
                    .map(Department::getName).orElse(null);
        }
        if (user.getSectionId() != null) {
            sectionName = sectionRepository.findByIdAndDeletedFalse(user.getSectionId())
                    .map(Section::getName).orElse(null);
        }
        if (user.getDesignationId() != null) {
            designationName = designationRepository.findByIdAndDeletedFalse(user.getDesignationId())
                    .map(Designation::getName).orElse(null);
        }
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .employeeCode(user.getEmployeeCode())
                .departmentId(user.getDepartmentId())
                .departmentName(departmentName)
                .sectionId(user.getSectionId())
                .sectionName(sectionName)
                .designationId(user.getDesignationId())
                .designationName(designationName)
                .roles(roles)
                .permissions(permissions)
                .build();
    }
}
