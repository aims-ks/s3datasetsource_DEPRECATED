package thredds.server.harvester;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import thredds.util.TdsPathUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Generate THREDDS catalog.xml configuration for given S3 buckets
 *
 * handles /s3Harvester/*
 */
@Controller
@RequestMapping("/s3Harvester")
public class S3HarvesterController {

    @RequestMapping("**")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO Security?
        // http://localhost:8888/thredds/s3Harvester/potato/carrot


        // TODO
        // 1. Find the proper way to send an array of values (S3 Buckets) using REST API <=== Maybe I need a proper config file for that...
        // 2. Extract the bucket names from the reqPath
        // 3. Generate catalog.xml files for the given buckets (in "/tmp" maybe?)
        // 4. Delete old catalog.xml files for the given buckets (and move temp config from "/tmp" to the config folder)
        // 5. Reload THREDDS config (how??)

        String reqPath = TdsPathUtils.extractPath(request, "s3Harvester/");

        System.out.println(reqPath);
    }
}
