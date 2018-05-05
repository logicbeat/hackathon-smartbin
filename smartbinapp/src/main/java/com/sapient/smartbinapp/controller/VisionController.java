package com.sapient.smartbinapp.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

@RestController
public class VisionController {

	@Autowired
	private ImageAnnotatorClient imageAnnotatorClient;

	@PostMapping("/vision")
	public String uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
		byte[] imageBytes = StreamUtils.copyToByteArray(file.getInputStream());
		Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();

		// Sets the type of request to label detection, to detect broad sets of
		// categories in an image.
		Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
		AnnotateImageRequest request = AnnotateImageRequest.newBuilder().setImage(image).addFeatures(feature).build();
		BatchAnnotateImagesResponse responses = this.imageAnnotatorClient.batchAnnotateImages(Collections.singletonList(request));
		StringBuilder responseBuilder = new StringBuilder();
		// We're only expecting one response.
		if (responses.getResponsesCount() == 1) {
			AnnotateImageResponse response = responses.getResponses(0);
			if (response.hasError()) {
				System.out.println(response.getError());
				throw new Exception(response.getError().getMessage());
			}
			final List<String> labelList = response.getLabelAnnotationsList().stream()
					.map(EntityAnnotation::getDescription).collect(Collectors.toList());

			responseBuilder.append("<div>");
			if (labelList.contains("fruit")) {
				responseBuilder.append("bio-degradable");
			} else if (labelList.contains("bottle")) {
				responseBuilder.append("non-bio-degradable");
			} else {
				responseBuilder.append("<table>");
				responseBuilder.append("<tr><th>description</th><th>score</th></tr>");
				for (EntityAnnotation annotation : response.getLabelAnnotationsList()) {
					responseBuilder.append("<tr><td>").append(annotation.getDescription()).append("</td><td>")
							.append(annotation.getScore()).append("</td></tr>");
				}
				responseBuilder.append("</table>");
			}
		}
		responseBuilder.append("</div>");

		return responseBuilder.toString();
	}
}
