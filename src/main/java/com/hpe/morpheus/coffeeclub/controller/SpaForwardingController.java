package com.hpe.morpheus.coffeeclub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React single-page application for any non-API, non-asset path so that a browser
 * refresh on a deep link still lands on the app instead of a 404. Only relevant for the packaged
 * JAR and container builds, where the compiled frontend lives in {@code /static}.
 */
@Controller
public class SpaForwardingController {

    @GetMapping({"/", "/{path:[^.]*}"})
    public String forwardToSinglePageApp() {
        return "forward:/index.html";
    }
}
