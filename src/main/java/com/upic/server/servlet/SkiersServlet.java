package com.upic.server.servlet;

import com.google.gson.Gson;
import com.upic.server.model.RideInfo;
import com.upic.server.model.ResponseMessage;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implement the POST API for the /skiers endpoint using Java servlets
 * This servlet only process Post request, return 404 status code for Get request
 */
@WebServlet(name = "SkiersServlet", urlPatterns = "/skiers")
public class SkiersServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        Gson gson = new Gson();
        try {
            // Handle JSon request
            StringBuilder stringBuilder = new StringBuilder();
            String temp;
            while ((temp = request.getReader().readLine()) != null) {
                stringBuilder.append(temp);
            }
            // Convert to Java object
            RideInfo rideInfo = gson.fromJson(stringBuilder.toString(), RideInfo.class);

            // Missing ride info
            if(rideInfo == null || rideInfo.getSkierID() == null || rideInfo.getResortID() == null
                    || rideInfo.getLiftID() == null || rideInfo.getSeasonID() == null
                    || rideInfo.getDayID() == null || rideInfo.getTime() == null){
                response.setStatus(404);
                response.getOutputStream().print(gson.toJson(new ResponseMessage("Data not found")));
            }else{
                // Ride info invalid
                if (rideInfo.getSkierID() < 1 || rideInfo.getSkierID() > 100000
                        || rideInfo.getResortID() < 1 || rideInfo.getResortID() > 10
                        || rideInfo.getLiftID() < 1 || rideInfo.getLiftID() > 40
                        || !rideInfo.getSeasonID().equals("2024") || !rideInfo.getDayID().equals("1")
                        || rideInfo.getTime() < 1 || rideInfo.getTime() > 360) {
                    response.setStatus(400);
                    response.getOutputStream().print(gson.toJson(new ResponseMessage("Invalid inputs")));
                } else {
                    // Success
                    response.setStatus(201);
                    response.getOutputStream().print(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getOutputStream().print(gson.toJson(new ResponseMessage("Server Error")));
        }finally {
            response.getOutputStream().flush();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        response.setContentType("application/json");
        response.getOutputStream().print(gson.toJson(new ResponseMessage("Error Request Method")));
        response.setStatus(404);
        response.getOutputStream().flush();
    }
}
