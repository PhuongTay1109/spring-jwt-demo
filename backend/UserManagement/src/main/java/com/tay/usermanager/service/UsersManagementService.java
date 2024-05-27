package com.tay.usermanager.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tay.usermanager.dto.ReqRes;
import com.tay.usermanager.model.Users;
import com.tay.usermanager.repository.UsersRepo;
import com.tay.usermanager.util.JWTUtils;

@Service
public class UsersManagementService {

	@Autowired
	private UsersRepo usersRepo;
	@Autowired
	private JWTUtils jwtUtils;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private PasswordEncoder passwordEncoder;

	public ReqRes register(ReqRes registrationRequest) {
		ReqRes resp = new ReqRes();
		
		Optional<Users> userOptional = usersRepo.findByEmail(registrationRequest.getEmail());
		if (userOptional.isPresent()) {
			resp.setStatusCode(409); // conflict
			resp.setMessage("User with this email has already existed");
			return resp;
		}
		
		try {
			Users user = new Users();
			user.setEmail(registrationRequest.getEmail());
			user.setCity(registrationRequest.getCity());
			user.setRole(registrationRequest.getRole());
			user.setName(registrationRequest.getName());
			user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));

			Users userResult = usersRepo.save(user);
			if (userResult.getId() > 0) {
				resp.setUser(userResult);
				resp.setMessage("User saved successfully");
				resp.setStatusCode(200);
			}
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setError(e.getMessage());
		}
		return resp;
	}

	public ReqRes login(ReqRes loginRequest) {
		ReqRes resp = new ReqRes();
		
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
			Users user = usersRepo.findByEmail(loginRequest.getEmail()).orElseThrow();
			String jwt = jwtUtils.generateToken(user);
			String refreshToken = jwtUtils.generateRefreshToken(new HashMap<>(), user);

			resp.setStatusCode(200);
			resp.setToken(jwt);
			resp.setRefreshToken(refreshToken);
			resp.setRole(user.getRole());
			resp.setExpirationTime("24Hrs");
			resp.setMessage("Successfully logged in");
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage(e.getMessage());
		}

		return resp;
	}

	public ReqRes refreshToken(ReqRes refreshTokenRequest) {
		ReqRes resp = new ReqRes();

		try {
			String email = jwtUtils.extractUsername(refreshTokenRequest.getToken());
			Users user = usersRepo.findByEmail(email).orElseThrow();

			if (jwtUtils.isTokenValid(refreshTokenRequest.getToken(), user)) {
				String jwt = jwtUtils.generateToken(user);
				resp.setStatusCode(200);
				resp.setToken(jwt);
				resp.setRefreshToken(refreshTokenRequest.getToken());
				resp.setExpirationTime("24Hrs");
				resp.setMessage("Successfully refreshed token");
			}
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage(e.getMessage());
		}

		return resp;
	}

	public ReqRes getAllUsers() {
		ReqRes resp = new ReqRes();

		try {
			List<Users> result = usersRepo.findAll();
			if (!result.isEmpty()) {
				resp.setUsersList(result);
				resp.setStatusCode(200);
				resp.setMessage("Successful");
			} else {
				resp.setStatusCode(404);
				resp.setMessage("No users found");
			}
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage("Error occurred: " + e.getMessage());
		}

		return resp;
	}

	public ReqRes getUserById(Integer userId) {
		ReqRes resp = new ReqRes();

		try {
			Users userById = usersRepo.findById(userId).orElseThrow(() -> new RuntimeException("User Not found"));
			resp.setUser(userById);
			resp.setStatusCode(200);
			resp.setMessage("User with id '" + userId + "' found successfully");
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage("Error occurred: " + e.getMessage());
		}

		return resp;
	}

	public ReqRes deleteUser(Integer userId) {
		ReqRes resp = new ReqRes();

		try {
			Optional<Users> userOptional = usersRepo.findById(userId);
			if (userOptional.isPresent()) {
				usersRepo.deleteById(userId);
				resp.setStatusCode(200);
				resp.setMessage("User deleted successfully");
			} else {
				resp.setStatusCode(404);
				resp.setMessage("User not found for deletion");
			}
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage("Error occurred while deleting user: " + e.getMessage());
		}

		return resp;
	}

	public ReqRes updateUser(Integer userId, Users updatedUser) {
		ReqRes resp = new ReqRes();
		
		try {
			Optional<Users> userOptional = usersRepo.findById(userId);
			if (userOptional.isPresent()) {
				Users existingUser = userOptional.get();
				existingUser.setEmail(updatedUser.getEmail());
				existingUser.setName(updatedUser.getName());
				existingUser.setCity(updatedUser.getCity());
				existingUser.setRole(updatedUser.getRole());

				// Check if password is present in the request
				if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
					// Encode the password and update it
					existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
				}

				Users savedUser = usersRepo.save(existingUser);
				resp.setUser(savedUser);
				resp.setStatusCode(200);
				resp.setMessage("User updated successfully");
			} else {
				resp.setStatusCode(404);
				resp.setMessage("User not found for update");
			}
		} catch (Exception e) {
			resp.setStatusCode(500);
			resp.setMessage("Error occurred while updating user: " + e.getMessage());
		}
		
		return resp;
	}
	
	public ReqRes getUserInfo(String email){
        ReqRes resp = new ReqRes();
        
        try {
            Optional<Users> userOptional = usersRepo.findByEmail(email);
            if (userOptional.isPresent()) {
                resp.setUser(userOptional.get());
                resp.setStatusCode(200);
                resp.setMessage("successful");
            } else {
                resp.setStatusCode(404);
                resp.setMessage("User info not found");
            }

        }catch (Exception e){
            resp.setStatusCode(500);
            resp.setMessage("Error occurred while getting user info: " + e.getMessage());
        }
        
        return resp;
    }
}