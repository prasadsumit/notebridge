package com.prasadsumit.notebridge;

import org.junit.jupiter.api.Test;
import com.prasadsumit.notebridge.web.HomeController;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
class NotebridgeApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("landing page renders")
	void landingPageRenders() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("home"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Make your notes")));
	}

}
