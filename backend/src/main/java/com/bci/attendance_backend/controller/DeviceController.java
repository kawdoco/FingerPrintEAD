package com.bci.attendance_backend.controller;

public class DeviceController {
    
}
package com.bci.attendance_backend.controller;

import com.bci.attendance_backend.entity.Device;
import com.bci.attendance_backend.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    @Autowired
    private DeviceService deviceService;

    @GetMapping
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @PostMapping
    public Device registerDevice(@RequestBody Device device) {
        return deviceService.registerDevice(device);
    }
}