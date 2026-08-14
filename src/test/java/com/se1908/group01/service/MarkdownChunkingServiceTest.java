package com.se1908.group01.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownChunkingServiceTest {

	private final MarkdownChunkingService service =
			new MarkdownChunkingService();

	@Test
	void preservesParentHeadingsForEachSection() {
		var markdown = """
				## I. Overview

				### 1. Introduction

				#### 1.1. High-Level Overview

				Overview content that belongs to the first subsection.

				#### 1.2. Users & Primary Actors

				Users and actors introduction content.

				##### 1. Human Actors

				Human actors content.

				##### 2. External Systems

				External systems content.
				""";

		var chunks = service.chunk(markdown, 300);

		assertEquals(4, chunks.size());
		assertTrue(chunks.get(1).getContent().contains(
				"#### 1.2. Users & Primary Actors"
		));
		assertTrue(chunks.get(2).getContent().contains(
				"#### 1.2. Users & Primary Actors"
		));
		assertTrue(chunks.get(2).getContent().contains(
				"##### 1. Human Actors"
		));
		assertTrue(chunks.get(3).getContent().contains(
				"##### 2. External Systems"
		));
	}
}
