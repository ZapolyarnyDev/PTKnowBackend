package ptknow.service.auth;

import ptknow.dto.auth.LoginDTO;
import ptknow.dto.auth.RegistrationDTO;
import ptknow.model.auth.Auth;
import ptknow.exception.email.EmailAlreadyUsedException;
import ptknow.exception.email.EmailNotFoundException;
import ptknow.exception.credentials.InvalidCredentialsException;
import ptknow.repository.auth.AuthRepository;
import ptknow.service.profile.ProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService implements UserDetailsService {

    AuthRepository repository;
    PasswordEncoder passwordEncoder;
    ProfileService profileService;
    RecaptchaVerificationService recaptchaVerificationService;

    @Transactional
    public Auth register(String fullName, String email, String password) {
        if(repository.existsByEmail(email))
            throw new EmailAlreadyUsedException(email);

        String hashedPassword = passwordEncoder.encode(password);

        var entity = Auth.builder()
                .email(email)
                .password(hashedPassword)
                .build();

        repository.save(entity);
        profileService.createProfile(fullName, entity);
        log.info("User registered successfully. Full name: {}; Email: {}", fullName, email);
        return entity;
    }

    @Transactional
    public Auth register(RegistrationDTO registrationDTO) {
        recaptchaVerificationService.verifyForRegistration(registrationDTO.recaptchaToken());
        return register(registrationDTO.fullName(), registrationDTO.email(), registrationDTO.password());
    }

    @Transactional
    public Auth authenticate(String email, String password) {
        var entity = loadUserByUsername(email);

        if (!entity.isEnabled())
            throw new AccessDeniedException("User account is blocked");

        if(!passwordEncoder.matches(password, entity.getPassword()))
            throw new InvalidCredentialsException("Invalid email or password");

        log.info("User log in successfully. Email: {}", email);
        return entity;
    }

    @Transactional
    public Auth authenticate(LoginDTO loginDTO) {
        recaptchaVerificationService.verifyForLogin(loginDTO.recaptchaToken());
        return authenticate(loginDTO.email(), loginDTO.password());
    }

    @Override
    public Auth loadUserByUsername(String email) throws UsernameNotFoundException {
        return repository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException(email));
    }

}
