package com.quezap.application.api.v1.routes.questions;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quezap.application.api.v1.dto.questions.QuestionShortInfoDto;

@RestController("questions")
public class ListQuestions {
  @GetMapping("find")
  List<QuestionShortInfoDto> find() {

    return List.of();
  }
}
