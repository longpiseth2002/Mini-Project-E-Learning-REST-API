package co.istad.elearning.features.student;

import co.istad.elearning.domain.*;
import co.istad.elearning.features.citycountry.CityRepository;
import co.istad.elearning.features.citycountry.CountryRepository;
import co.istad.elearning.features.student.dto.StudentCreateRequest;
import co.istad.elearning.features.student.dto.StudentResponse;
import co.istad.elearning.features.user.RoleRepository;
import co.istad.elearning.features.user.UserRepository;
import co.istad.elearning.features.user.dto.UserCreateRequest;
import co.istad.elearning.features.user.dto.UserDetailResponse;
import co.istad.elearning.features.user.dto.UserResponse;
import co.istad.elearning.mapper.StudentMapper;
import co.istad.elearning.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService{

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepositor;
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;

    @Override
    public StudentResponse createNew(UserCreateRequest userCreateRequest, StudentCreateRequest studentCreateRequest) {
        //create new user
        if (userRepository.existsByPhoneNumber(userCreateRequest.phoneNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone number has already been existed!"
            );
        }

        if (userRepository.existsByNationalIdCard(userCreateRequest.nationalIdCard())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "National card ID has already been existed!"
            );
        }

        if(userRepository.existsByEmail(userCreateRequest.email())){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email has already been existed!"
            );
        }

        if (!userCreateRequest.password()
                .equals(userCreateRequest.confirmedPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password doesn't match!"
            );
        }

        User user = userMapper.fromUserCreateRequest(userCreateRequest);
        user.setUuid(UUID.randomUUID().toString());
        user.setIsDeleted(false);
        user.setIsVerified(true);

        City city = cityRepository.findById(userCreateRequest.cityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found with id " + userCreateRequest.cityId()));
        Country country = countryRepositor.findById(userCreateRequest.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found with id " + userCreateRequest.countryId()));
        user.setCountry(country);
        user.setCity(city);

        //this is default profile picture
        user.setProfile("097213a0-9838-4bd5-851c-fa55e2fa3ddd.png");

        // Assign default user role as USER and student
        List<Role> roles = new ArrayList<>();

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Role USER has not been found!"));

        roles.add(userRole);
        user.setRoles(roles);

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Role STUDENT has not been found!"
                        ));

        roles.add(studentRole);
        user.setRoles(roles);

        userRepository.save(user);

        //Create new student
        Student student = studentMapper.fromStudentCreateRequest(studentCreateRequest);
        student.setIsBlocked(false);
        student.setUser(user);

        studentRepository.save(student);

        return studentMapper.toStudentResponse(student);
    }

    @Override
    public Page<StudentResponse> findList(int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        Page<Student> students = studentRepository.findAll(pageRequest);

        return students.map(studentMapper::toStudentResponse);
    }

    private String generateImageUrl(HttpServletRequest request, String filename) {
        return String.format("%s://%s:%d/images/%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                filename);
    }

    @Override
    public UserResponse findProfileByUsername(String username, HttpServletRequest request) {

        Role role = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role name"));

        User foundUser = userRepository.findByUsernameAndRoles(username,role)
                .orElseThrow(() -> (
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Student's username has not been found!" )
                ));

        String profileImageUrl = generateImageUrl(request, foundUser.getProfile());

        return new UserResponse(foundUser.getUsername(), profileImageUrl);
    }


    @Override
    public String updateProfile(String username,String mediaName,HttpServletRequest request) {

        Role role = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role name"));

        User foundUser = userRepository.findByUsernameAndRoles(username,role)
                .orElseThrow(() -> (
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Student's username has not been found!" )
                ));

        foundUser.setProfile(mediaName);

        userRepository.save(foundUser);
        return generateImageUrl(request,mediaName);
    }


}
