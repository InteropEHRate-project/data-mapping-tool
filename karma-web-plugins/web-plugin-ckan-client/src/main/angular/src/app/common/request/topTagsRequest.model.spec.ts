import {TopTagsRequest} from "./topTagsRequest.model";


describe('TopTagsRequest', () => {
  let topTagsRequest: TopTagsRequest;

  afterEach(() => {
    topTagsRequest = null;
  });

  /**
   * #getQueryParameters()
   * test empty parameter request
   */
  it('#getQueryParameters(): empty parameter request',
    () => {
      topTagsRequest = new TopTagsRequest();
      expect(topTagsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags"]&rows=0';
      expect(topTagsRequest.getQueryParameters()).toBe(parameters);
    }
  );

  /**
   * #getQueryParameters()
   * test top 10 parameter request
   */
  it('#getQueryParameters(): top 10 tags parameter request',
    () => {
      topTagsRequest = new TopTagsRequest({
        facet_limit: 10
      });
      expect(topTagsRequest).toBeTruthy();
      const parameters = 'facet.field=["tags"]&facet.limit=10&rows=0';
      expect(topTagsRequest.getQueryParameters()).toBe(parameters);
    }
  );


});
