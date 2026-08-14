package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

@ExtendWith(MockitoExtension.class)
class DocumentEmbeddingServiceTest {

	@Mock
	private EmbeddingModel embeddingModel;

	@Test
	void embedVectorsSplitsLargeInputIntoSafeBatches() {
		when(embeddingModel.embedForResponse(anyList())).thenAnswer(invocation -> {
			List<String> batch = invocation.getArgument(0);
			var embeddings = IntStream.range(0, batch.size())
					.mapToObj(index -> new Embedding(new float[]{index, 1.0F}, index))
					.toList();
			return new EmbeddingResponse(embeddings);
		});
		var service = new DocumentEmbeddingService(embeddingModel, new ObjectMapper());
		var texts = IntStream.range(0, 200).mapToObj(index -> "chunk-" + index).toList();

		var vectors = service.embedVectors(texts);

		assertEquals(200, vectors.size());
		var batches = ArgumentCaptor.<List<String>>captor();
		verify(embeddingModel, org.mockito.Mockito.times(3)).embedForResponse(batches.capture());
		assertEquals(List.of(90, 90, 20), batches.getAllValues().stream().map(List::size).toList());
		assertEquals("title: none | text: chunk-0", batches.getAllValues().getFirst().getFirst());
		assertEquals("title: none | text: chunk-199", batches.getAllValues().getLast().getLast());
	}

	@Test
	void embedQuestionAddsQuestionAnsweringInstruction() {
		when(embeddingModel.embedForResponse(anyList()))
				.thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[]{0.1F}, 0))));
		var service = new DocumentEmbeddingService(embeddingModel, new ObjectMapper());

		service.embedQuestion("What is this document about?");

		var batch = ArgumentCaptor.<List<String>>captor();
		verify(embeddingModel).embedForResponse(batch.capture());
		assertEquals(
				List.of("task: question answering | query: What is this document about?"),
				batch.getValue()
		);
	}
}
