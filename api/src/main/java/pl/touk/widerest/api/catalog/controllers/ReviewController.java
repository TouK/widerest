package pl.touk.widerest.api.catalog.controllers;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/reviews")
@Api
public class ReviewController {

    /*
    @Resource(name = "blRatingService")
    protected RatingService ratingService;

    @Resource(name="blCatalogService")
    protected CatalogService catalogService;

    @ApiOperation("readProductReviews")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("permitAll")
    public List<Review> readProductReviews(@PathVariable(value="id") String id) {

        RatingSummary ratingSummary = Optional.ofNullable(ratingService.readRatingSummary(id, RatingType.PRODUCT))
                .orElseThrow(ResourceNotFoundException::new);

        return Optional.ofNullable(ratingSummary.getReviews().stream()
                .map(DtoConverter.reviewDetailEntityToDto).collect(Collectors.toList())).orElseThrow(ResourceNotFoundException::new);

    }
    */
    /*
    @ApiOperation("saveProductReview")
    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    public void saveProductReview(@PathVariable(value = "id") String id, @RequestBody Review productReview) {
*/


        /*

        Optional.ofNullable(catalogService.findProductById(id)).orElseThrow(ResourceNotFoundException::new);

        RatingSummary ratingSummary = Optional.ofNullable(ratingService.readRatingSummary(id.toString(), RatingType.PRODUCT))
                .orElse(new RatingSummaryImpl());

        ratingSummary.getReviews().add(dtoToReviewDetailEntity.apply(productReview));


        ratingService.saveRatingSummary(ratingService.saveRatingSummary(ratingSummary));
        */

}