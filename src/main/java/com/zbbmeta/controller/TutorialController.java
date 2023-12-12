package com.zbbmeta.controller;


import com.zbbmeta.annotation.DataSource;
import com.zbbmeta.config.DatabaseType;
import com.zbbmeta.entity.Tutorial;
import com.zbbmeta.service.TutorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TutorialController {


    @Autowired
    private TutorialService tutorialService;


    @DataSource
    @GetMapping("/list")
    public List<Tutorial> list(){
        return tutorialService.list();

    }

    @DataSource(type = DatabaseType.MASTER)
    @GetMapping("/create")
    public Boolean create(){

        Tutorial tutorial = new Tutorial();
        tutorial.setTitle("master");
        tutorial.setDescription("master");

        return tutorialService.save(tutorial);
    }
}
