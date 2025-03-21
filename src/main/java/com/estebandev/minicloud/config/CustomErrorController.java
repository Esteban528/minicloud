package com.estebandev.minicloud.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/error")
public class CustomErrorController extends BasicErrorController {

    @Value("${var.env}")
    private String env;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<HttpStatus, String> ERROR_MESSAGES = Map.ofEntries(
            Map.entry(HttpStatus.BAD_REQUEST,
                    "Oops! Something is wrong with your request. Please check and try again."),
            Map.entry(HttpStatus.UNAUTHORIZED, "You need to log in to access this resource."),
            Map.entry(HttpStatus.FORBIDDEN, "You don’t have permission to access this page."),
            Map.entry(HttpStatus.NOT_FOUND, "Sorry, we couldn’t find what you were looking for."),
            Map.entry(HttpStatus.METHOD_NOT_ALLOWED, "This action is not supported on this resource."),
            Map.entry(HttpStatus.REQUEST_TIMEOUT, "Your request took too long. Please try again."),
            Map.entry(HttpStatus.CONFLICT, "There is a conflict with your request. Please resolve it and retry."),
            Map.entry(HttpStatus.GONE, "This resource is no longer available."),
            Map.entry(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too big! Try uploading a smaller one."),
            Map.entry(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "We can't process this file format. Try a different one."),
            Map.entry(HttpStatus.TOO_MANY_REQUESTS, "Slow down! Too many requests. Please wait and try again later."),
            Map.entry(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our end. Please try again later."),
            Map.entry(HttpStatus.BAD_GATEWAY, "We’re having trouble connecting to the service. Try again soon."),
            Map.entry(HttpStatus.SERVICE_UNAVAILABLE,
                    "The service is temporarily unavailable. Please try again later."));

    public CustomErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties) {
        super(errorAttributes, serverProperties.getError());
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> errorAttributes = getErrorAttributes(request,
                getErrorAttributeOptions(request, MediaType.TEXT_HTML));

        HttpStatus status = getStatus(request);

        String originalMessage = (String) errorAttributes.getOrDefault("message", "");
        String originalError = (String) errorAttributes.getOrDefault("error", "An error has ocurred");
        String originalTrace = (String) errorAttributes.getOrDefault("trace", "");

        String error = env.equals("dev") ? originalError
                : getErrorMessage(status);

        String message = env.equals("dev") ? originalMessage
                : "";

        String trace = env.equals("dev") ? originalTrace : "";

        ModelAndView model = new ModelAndView("errorpage", status);
        model.addObject("status", status);
        model.addObject("error", error);
        model.addObject("message", message);
        model.addObject("trace", trace);

        logger.error(originalError);
        logger.error("Error ocurred in {} \nMessage: {}", request.getRequestURL(), originalMessage);
        logger.trace(originalTrace);
        return model;
    }

    public static String getErrorMessage(HttpStatus status) {
        return ERROR_MESSAGES.getOrDefault(status, "An unexpected error occurred.");
    }
}
