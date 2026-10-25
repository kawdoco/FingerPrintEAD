package com.bcilab.attendance.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the React build is packed into the jar (backend/src/main/resources/static),
 * a browser refresh on /admin/people or /admin/login must still load the React app,
 * because those routes only exist inside React Router. Forward them to index.html.
 * Has no effect while developing with `npm run dev` (Vite serves the pages then).
 */
@Controller
public class SpaController {

    @GetMapping({"/admin", "/admin/{page}"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
