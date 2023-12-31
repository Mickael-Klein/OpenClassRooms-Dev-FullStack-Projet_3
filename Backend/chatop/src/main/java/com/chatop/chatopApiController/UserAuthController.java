package com.chatop.chatopApiController;

import com.chatop.Interface.ChatopApiInterface.UserInterface;
import com.chatop.Interface.UtilEntityAndDTOCreationInterface.EntityAndDTOCreationInterface;
import com.chatop.Interface.UtilResponseInterface.UserResponseInterface;
import com.chatop.chatopApiDTO.UserDTO;
import com.chatop.chatopApiModel.DbUser;
import com.chatop.chatopApiService.JWTServiceImpl;
import com.chatop.utils.RequestInput.LoginRequestInput;
import com.chatop.utils.RequestInput.RegisterRequestInput;
import com.chatop.utils.SwaggerApiResponse.SwaggerApiJWTTokenResponseModel;
import com.chatop.utils.SwaggerApiResponse.SwaggerApiMessageResponseModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

  @Autowired
  private UserInterface userService;

  @Autowired
  private JWTServiceImpl jwtService;

  @Autowired
  UserResponseInterface userResponseComponent;

  @Autowired
  EntityAndDTOCreationInterface entityAndDTOCreationComponent;

  /**
   * Constructs a new UserAuthController with the specified JWTService.
   *
   * @param jwtService The JWTService instance.
   */
  public UserAuthController(JWTServiceImpl jwtService) {
    this.jwtService = jwtService;
  }

  /**
   * Registers a new user account.
   *
   * @param registerRequestUser The registration request payload.
   * @param bindingResult       The result of the request payload validation.
   * @return ResponseEntity containing the result of the registration operation.
   */
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Registering user success",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiJWTTokenResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Bad request, incorrect registering datas",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
    }
  )
  @PostMapping("/register")
  public ResponseEntity<Object> registerAccount(
    @Valid @RequestBody RegisterRequestInput registerRequestUser,
    BindingResult bindingResult
  ) {
    try {
      boolean isRequestPayloadInvalid = bindingResult.hasErrors();
      if (isRequestPayloadInvalid) {
        return ResponseEntity
          .badRequest()
          .body(userResponseComponent.getRegisteringBadCredentialsJsonString());
      }

      boolean emailAlreadyRegistered = userService.isEmailAlreadyUsed(
        registerRequestUser.getEmail()
      );
      if (emailAlreadyRegistered) {
        return ResponseEntity
          .badRequest()
          .body(
            userResponseComponent.getRegisteringEmailAlreadyUsedJsonString()
          );
      }

      DbUser factoryUser = entityAndDTOCreationComponent.getFactoryUserRegisterEntity(
        registerRequestUser
      );
      DbUser user = userService.saveUser(factoryUser);

      String jwtToken = jwtService.generateToken(user.getId());

      return ResponseEntity
        .ok()
        .body(userResponseComponent.getJwtTokenJsonString(jwtToken));
    } catch (Exception e) {
      return ResponseEntity
        .badRequest()
        .body(userResponseComponent.getRegisteringErrorJsonString());
    }
  }

  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Login success",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiJWTTokenResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Bad request, wrong credentials",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
    }
  )
  @PostMapping("/login")
  public ResponseEntity<Object> login(
    @Valid @RequestBody LoginRequestInput request,
    BindingResult bindingResult
  ) {
    try {
      boolean isRequestPayloadInvalid = bindingResult.hasErrors();
      if (isRequestPayloadInvalid) {
        return handleInvalidCredentials();
      }

      Optional<DbUser> optionalUser = userService.getUserByEmail(
        request.getEmail()
      );
      if (!optionalUser.isPresent()) {
        return handleInvalidCredentials();
      }

      DbUser user = optionalUser.get();

      boolean isPasswordCorrect = userService.isPasswordValid(
        request.getPassword(),
        user
      );
      if (!isPasswordCorrect) {
        return handleInvalidCredentials();
      }

      String jwtToken = jwtService.generateToken(user.getId());

      return ResponseEntity
        .ok()
        .body(userResponseComponent.getJwtTokenJsonString(jwtToken));
    } catch (Exception e) {
      return ResponseEntity
        .badRequest()
        .body(userResponseComponent.getLoginErrorJsonString());
    }
  }

  private ResponseEntity<Object> handleInvalidCredentials() {
    return ResponseEntity
      .badRequest()
      .body(userResponseComponent.getLoginBadCredentialsJsonString());
  }

  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Get user success",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UserDTO.class)
          ),
        }
      ),
      @ApiResponse(
        responseCode = "400",
        description = "Bad request, user not retrieve with jwt provided",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "401",
        description = "Unauthorize",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
      @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(
              implementation = SwaggerApiMessageResponseModel.class
            )
          ),
        }
      ),
    }
  )
  @Operation(security = { @SecurityRequirement(name = "bearer-key") })
  @GetMapping("/me")
  public ResponseEntity<Object> getMe(@AuthenticationPrincipal Jwt jwt) {
    try {
      long userId = jwtService.getUserIdFromJwtlong(jwt);
      Optional<DbUser> optionalUser = userService.getUserById(userId);
      if (!optionalUser.isPresent()) {
        return ResponseEntity
          .badRequest()
          .body(userResponseComponent.getJwtInvalidJwtJsonString());
      }
      DbUser user = optionalUser.get();

      UserDTO userDTO = entityAndDTOCreationComponent.getFactoryUserDTO(user);

      return ResponseEntity.ok().body(userDTO);
    } catch (Exception e) {
      return ResponseEntity
        .badRequest()
        .body(userResponseComponent.getJwtInvalidJwtJsonString());
    }
  }
}
