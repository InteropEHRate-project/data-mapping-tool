import {fakeAsync, TestBed, tick} from '@angular/core/testing';

import {SearchService} from './search.service';
import {AppConfigService} from "../app-config.service";
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";
import {DatasetsSearchRequest} from "./request/datasetsSearchRequest.model";
import {Dictionary} from "./dictionary.model";
import {Filter} from "./filter.model";
import {Dataset} from "./dataset.model";
import {Resource} from "./resource.model";
import {TopTagsRequest} from "./request/topTagsRequest.model";

describe('SearchService', () => {
  let service: SearchService;
  let appConfigServiceSpy: jasmine.SpyObj<AppConfigService>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    // spy for AppConfigService
    const spy = jasmine.createSpyObj('AppConfigService', ['config']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SearchService,
        {provide: AppConfigService, useValue: spy},
      ]
    });
    //inject the service
    service = TestBed.inject(SearchService);
    // use spy for AppConfigService
    appConfigServiceSpy = TestBed.inject(AppConfigService) as jasmine.SpyObj<AppConfigService>;
    // Inject the http service and test controller for each test
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // After every test, assert that there are no more pending requests.
    httpTestingController.verify();
  });

  /**
   * test if service created successfully
   */
  it('should be created',
    () => {
      expect(service).toBeTruthy();
      expect(appConfigServiceSpy).toBeTruthy();
      expect(httpTestingController).toBeTruthy();
    }
  );

  /**
   * [x] makeGetRequest() normal
   * [x] apiUrl
   * [ ] apiKey
   */
  it('#makeGetRequest should return a Observable of Object (no apiKey)',
    fakeAsync(() => {

      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: ''
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);

      // create dataset search request
      const request = new DatasetsSearchRequest();
      const response = 'testing';
      // testing method
      service.makeGetRequest(request).subscribe(value => {
        expect(value).toBe(response);
      });

      // Expect a call to this URL
      const req = httpTestingController.expectOne(
        'http://example.com' + encodeURIComponent('/api/3/action/package_search'
        + '?facet.field=["tags","organization","groups","res_format","resource_license_en"]')
      );
      // Assert that the request is a GET.
      expect(req.request.method).toEqual("GET");
      // Respond with this data when called
      req.flush(response);
      // Call tick which actually processes to response
      tick();

    })
  );

  /**
   * [x] makeGetRequest() error 400
   * [x] apiUrl
   * [ ] apiKey
   */
  it('#makeGetRequest should return a Observable of Object (no apiKey) (error) ',
    fakeAsync(() => {
      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: ''
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);
      // create dataset search request
      let request = new DatasetsSearchRequest();

      // testing method
      let response: any;
      let errResponse: any = '';
      const mockErrorResponse = {status: 400, statusText: 'Bad Request'};
      const data = 'Something bad happened; please try again later.';
      service.makeGetRequest(request).subscribe(res => response = res, err => errResponse = err);
      httpTestingController.expectOne('http://example.com'+ encodeURIComponent('/api/3/action/package_search'
        + '?facet.field=["tags","organization","groups","res_format","resource_license_en"]')
      ).flush(data, mockErrorResponse);
      expect(errResponse).toBe(data);
      // Call tick which actually processes te response
      tick();
    })
  );

  /**
   * [x] makeGetRequest() normal
   * [x] apiUrl
   * [x] apiKey
   */
  it('#makeGetRequest should return a Observable of Object (with apiKey)',
    fakeAsync(() => {

      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: '12345678'
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);

      // create dataset search request
      const request = new DatasetsSearchRequest();
      const response = 'testing with key';
      // testing method
      service.makeGetRequest(request).subscribe(value => {
        expect(value).toBe(response);
      });

      // Expect a call to this URL
      const req = httpTestingController.expectOne(
        'http://example.com' + encodeURIComponent('/api/3/action/package_search'
        + '?facet.field=["tags","organization","groups","res_format","resource_license_en"]'
        + '&include_private=true'
        + '&include_drafts=true')
      );
      // Assert that the request is a GET.
      expect(req.request.method).toEqual("GET");
      expect(req.request.headers.get('Authorization')).toEqual("12345678");
      // Respond with this data when called
      req.flush(response);
      // Call tick which actually processes to response
      tick();

    })
  );

  /**
   * [x] getDataSetsAndFilters() normal
   * [x] apiUrl
   * [ ] apiKey
   */
  it(`#getDataSetsAndFilters (normal)`,
    fakeAsync(() => {

      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: ''
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);

      const request = new DatasetsSearchRequest();
      const serverResponse = {
        "help": "http://example.com/api/3/action/help_show?name=package_search",
        "success": true,
        "result": {
          "count": 3,
          "sort": "score desc, metadata_modified desc",
          "facets": {
            "organization": {
              "knowdive_test": 1
            },
            "resource_license_en": {},
            "res_format": {
              "JSON": 1,
              "CSV": 1,
              "plain text": 1
            },
            "groups": {
              "roger": 1,
              "david": 2
            },
            "tags": {
              "russian": 2,
              "Flexible ": 2,
              "tolstoy": 1,
              "medical": 1
            }
          },
          "results": [
            {
              "license_title": "Creative Commons Attribution",
              "maintainer": "",
              "relationships_as_object": [],
              "private": false,
              "maintainer_email": "",
              "num_tags": 1,
              "id": "49948f66-422c-4eab-bf86-e551e15a2873",
              "metadata_created": "2020-07-23T09:29:34.898416",
              "metadata_modified": "2020-07-23T09:31:10.759923",
              "author": "",
              "author_email": "",
              "state": "active",
              "version": "",
              "creator_user_id": "e8c1f7e1-fbd5-4ae9-a94a-14cb83d6096b",
              "type": "dataset",
              "resources": [
                {
                  "mimetype": "text/csv",
                  "cache_url": null,
                  "hash": "",
                  "description": "",
                  "name": "Biochemistry.csv",
                  "format": "CSV",
                  "url": "http://localhost:7000/dataset/49948f66-422c-4eab-bf86-e551e15a2873/resource/5b1508c9-3782-45cf-bb98-16fdc0c1dc9f/download/biochemistry.csv",
                  "datastore_active": false,
                  "cache_last_updated": null,
                  "package_id": "49948f66-422c-4eab-bf86-e551e15a2873",
                  "created": "2020-07-23T09:30:26.220449",
                  "state": "active",
                  "mimetype_inner": null,
                  "last_modified": "2020-07-23T09:30:26.148415",
                  "position": 0,
                  "revision_id": "8f57c6e3-6284-4415-9da1-64de6aa89334",
                  "url_type": "upload",
                  "id": "5b1508c9-3782-45cf-bb98-16fdc0c1dc9f",
                  "resource_type": null,
                  "size": 198921
                },
                {
                  "mimetype": "text/csv",
                  "cache_url": null,
                  "hash": "",
                  "description": "",
                  "name": "HospitalAdmissions.csv",
                  "format": "CSV",
                  "url": "file://///Users/mac/Desktop/01-Knowdive/Data/SPRINT-medicaldatasets/testData/HospitalAdmissions.csv",
                  "datastore_active": false,
                  "cache_last_updated": null,
                  "package_id": "49948f66-422c-4eab-bf86-e551e15a2873",
                  "created": "2020-07-23T09:31:10.173043",
                  "state": "active",
                  "mimetype_inner": null,
                  "last_modified": null,
                  "position": 1,
                  "revision_id": "0f17bb87-b392-4d82-8fd4-5ae40ee3cb86",
                  "url_type": null,
                  "id": "78feff74-2e6e-45d8-a8a7-fa38a2eabe35",
                  "resource_type": null,
                  "size": null
                }
              ],
              "num_resources": 2,
              "tags": [
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "medical",
                  "id": "66319603-e66e-47f2-96d2-8cea16069d76",
                  "name": "medical"
                }
              ],
              "groups": [],
              "license_id": "cc-by",
              "relationships_as_subject": [],
              "organization": {
                "description": "an organization to test the functionality of the ckan",
                "created": "2020-07-23T09:13:18.591081",
                "title": "knowdive_test",
                "name": "knowdive_test",
                "is_organization": true,
                "state": "active",
                "image_url": "",
                "revision_id": "d1a00f20-fabf-46d0-bc4e-d8cb5c7ea3a5",
                "type": "organization",
                "id": "2418b83f-4cbf-4824-9a5d-2175e0818cda",
                "approval_status": "approved"
              },
              "name": "knowdive_test_dataset_1",
              "isopen": true,
              "url": "",
              "notes": "",
              "owner_org": "2418b83f-4cbf-4824-9a5d-2175e0818cda",
              "extras": [],
              "license_url": "http://www.opendefinition.org/licenses/cc-by",
              "title": "knowdive_test_dataset_1",
              "revision_id": "0f17bb87-b392-4d82-8fd4-5ae40ee3cb86"
            },
            {
              "license_title": "Creative Commons Non-Commercial (Any)",
              "maintainer": null,
              "relationships_as_object": [],
              "private": false,
              "maintainer_email": null,
              "num_tags": 2,
              "id": "c361acc5-ad12-4e79-b6cf-860ae3876e70",
              "metadata_created": "2020-05-07T22:44:11.168880",
              "metadata_modified": "2020-05-07T22:44:11.168903",
              "author": null,
              "author_email": null,
              "state": "active",
              "version": null,
              "creator_user_id": null,
              "type": "dataset",
              "resources": [],
              "num_resources": 0,
              "tags": [
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "Flexible ",
                  "id": "2d300cb6-dfe0-4785-b0b7-6c19d524c439",
                  "name": "Flexible "
                },
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "russian",
                  "id": "aef20144-21fc-4c82-bd65-156c0dab9303",
                  "name": "russian"
                }
              ],
              "groups": [
                {
                  "display_name": "Dave's books",
                  "description": "These are books that David likes.",
                  "image_display_url": "",
                  "title": "Dave's books",
                  "id": "b2681536-ce77-48a9-9636-3d16e469a17d",
                  "name": "david"
                }
              ],
              "license_id": "cc-nc",
              "relationships_as_subject": [],
              "organization": null,
              "name": "warandpeace",
              "isopen": false,
              "url": null,
              "notes": null,
              "owner_org": null,
              "extras": [],
              "license_url": "http://creativecommons.org/licenses/by-nc/2.0/",
              "title": "A Wonderful Story",
              "revision_id": "ef73b43a-2986-450f-add5-01b90b7bf419"
            },
            {
              "license_title": "Other (Open)",
              "maintainer": null,
              "relationships_as_object": [],
              "private": false,
              "maintainer_email": null,
              "num_tags": 3,
              "id": "ec0d8dee-0a1b-44c8-a39c-e897fae7a797",
              "metadata_created": "2020-05-07T22:44:11.133593",
              "metadata_modified": "2020-05-07T22:44:11.133615",
              "author": null,
              "author_email": null,
              "state": "active",
              "version": "0.7a",
              "creator_user_id": null,
              "type": "dataset",
              "resources": [
                {
                  "mimetype": null,
                  "cache_url": null,
                  "state": "active",
                  "hash": "abc123",
                  "description": "Full text. Needs escaping: \" Umlaut: ü",
                  "last_modified": null,
                  "format": "plain text",
                  "url": "http://datahub.io/download/x=1&y=2",
                  "name": null,
                  "cache_last_updated": null,
                  "package_id": "ec0d8dee-0a1b-44c8-a39c-e897fae7a797",
                  "created": "2020-05-07T22:44:11.232757",
                  "size_extra": "123",
                  "mimetype_inner": null,
                  "url_type": null,
                  "position": 0,
                  "revision_id": "ef73b43a-2986-450f-add5-01b90b7bf419",
                  "datastore_active": false,
                  "id": "29947c7d-a0a4-4159-9036-cfaca0de6867",
                  "resource_type": null,
                  "size": null
                },
                {
                  "mimetype": null,
                  "cache_url": null,
                  "state": "active",
                  "hash": "def456",
                  "description": "Index of the novel",
                  "last_modified": null,
                  "format": "JSON",
                  "url": "http://datahub.io/index.json",
                  "name": null,
                  "cache_last_updated": null,
                  "package_id": "ec0d8dee-0a1b-44c8-a39c-e897fae7a797",
                  "created": "2020-05-07T22:44:11.232778",
                  "size_extra": "345",
                  "mimetype_inner": null,
                  "url_type": null,
                  "position": 1,
                  "revision_id": "ef73b43a-2986-450f-add5-01b90b7bf419",
                  "datastore_active": false,
                  "id": "cce176b3-8247-4771-9333-8e4b514ddd0a",
                  "resource_type": null,
                  "size": null
                }
              ],
              "num_resources": 2,
              "tags": [
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "Flexible ",
                  "id": "2d300cb6-dfe0-4785-b0b7-6c19d524c439",
                  "name": "Flexible "
                },
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "russian",
                  "id": "aef20144-21fc-4c82-bd65-156c0dab9303",
                  "name": "russian"
                },
                {
                  "vocabulary_id": null,
                  "state": "active",
                  "display_name": "tolstoy",
                  "id": "439d42aa-c34a-4478-9ff3-15e7ff66ae94",
                  "name": "tolstoy"
                }
              ],
              "groups": [
                {
                  "display_name": "Dave's books",
                  "description": "These are books that David likes.",
                  "image_display_url": "",
                  "title": "Dave's books",
                  "id": "b2681536-ce77-48a9-9636-3d16e469a17d",
                  "name": "david"
                },
                {
                  "display_name": "Roger's books",
                  "description": "Roger likes these books.",
                  "image_display_url": "",
                  "title": "Roger's books",
                  "id": "41808ede-362f-47eb-a92e-699377581f60",
                  "name": "roger"
                }
              ],
              "license_id": "other-open",
              "relationships_as_subject": [],
              "organization": null,
              "name": "annakarenina",
              "isopen": true,
              "url": "http://datahub.io",
              "notes": "Some test notes\n\n### A 3rd level heading\n\n**Some bolded text.**\n\n*Some italicized text.*\n\nForeign characters:\nu with umlaut ü\n66-style quote “\nforeign word: thümb\n\nNeeds escaping:\nleft arrow <\n\n<http://ckan.net/>\n\n",
              "owner_org": null,
              "extras": [
                {
                  "key": "genre",
                  "value": "romantic novel"
                },
                {
                  "key": "original media",
                  "value": "book"
                }
              ],
              "title": "A Novel By Tolstoy",
              "revision_id": "ef73b43a-2986-450f-add5-01b90b7bf419"
            }
          ],
          "search_facets": {
            "groups": {
              "items": [
                {
                  "count": 1,
                  "display_name": "Roger's books",
                  "name": "roger"
                },
                {
                  "count": 2,
                  "display_name": "Dave's books",
                  "name": "david"
                }
              ],
              "title": "groups"
            },
            "organization": {
              "items": [
                {
                  "count": 1,
                  "display_name": "knowdive_test",
                  "name": "knowdive_test"
                }
              ],
              "title": "organization"
            },
            "resource_license_en": {
              "items": [],
              "title": "resource_license_en"
            },
            "res_format": {
              "items": [
                {
                  "count": 1,
                  "display_name": "plain text",
                  "name": "plain text"
                },
                {
                  "count": 1,
                  "display_name": "JSON",
                  "name": "JSON"
                },
                {
                  "count": 1,
                  "display_name": "CSV",
                  "name": "CSV"
                }
              ],
              "title": "res_format"
            },
            "tags": {
              "items": [
                {
                  "count": 1,
                  "display_name": "tolstoy",
                  "name": "tolstoy"
                },
                {
                  "count": 2,
                  "display_name": "russian",
                  "name": "russian"
                },
                {
                  "count": 1,
                  "display_name": "medical",
                  "name": "medical"
                },
                {
                  "count": 2,
                  "display_name": "Flexible ",
                  "name": "Flexible "
                }
              ],
              "title": "tags"
            }
          }
        }
      };
      const methodResponse = new Dictionary({
        count: 3,
        datasets: [
          new Dataset({
            id: "49948f66-422c-4eab-bf86-e551e15a2873",
            title: "knowdive_test_dataset_1",
            resources: [
              new Resource({
                id: "5b1508c9-3782-45cf-bb98-16fdc0c1dc9f",
                resource_type: null,
                license_type: null,
                last_modified: "2020-07-23T09:30:26.148415",
                created: "2020-07-23T09:30:26.220449",
                name: "Biochemistry.csv",
                format: "CSV",
                url: "http://localhost:7000/dataset/49948f66-422c-4eab-bf86-e551e15a2873/resource/5b1508c9-3782-45cf-bb98-16fdc0c1dc9f/download/biochemistry.csv",
                description: null,
                state: null,
                size: 198921,
                distribution_format: null
              }),
              new Resource({
                id: "78feff74-2e6e-45d8-a8a7-fa38a2eabe35",
                resource_type: null,
                license_type: null,
                last_modified: null,
                created: "2020-07-23T09:31:10.173043",
                name: "HospitalAdmissions.csv",
                format: "CSV",
                url: "file://///Users/mac/Desktop/01-Knowdive/Data/SPRINT-medicaldatasets/testData/HospitalAdmissions.csv",
                description: null,
                state: null,
                size: null,
                distribution_format: null,
                selectedForImport: true
              })
            ],
            notes: null,
            url: null,
            license_title: "Creative Commons Attribution",
            author: null,
            type: "dataset"
          }),
          new Dataset({
            id: "c361acc5-ad12-4e79-b6cf-860ae3876e70",
            title: "A Wonderful Story",
            resources: [],
            notes: null,
            url: null,
            license_title: "Creative Commons Non-Commercial (Any)",
            author: null,
            type: "dataset",
            isShowMore: false
          }),
          new Dataset({
            id: "ec0d8dee-0a1b-44c8-a39c-e897fae7a797",
            title: "A Novel By Tolstoy",
            resources: [
              new Resource({
                id: "29947c7d-a0a4-4159-9036-cfaca0de6867",
                resource_type: null,
                license_type: null,
                last_modified: null,
                created: "2020-05-07T22:44:11.232757",
                name: null,
                format: "plain text",
                url: "http://datahub.io/download/x=1&y=2",
                description: "Full text. Needs escaping: \" Umlaut: ü",
                state: null,
                size: null,
                distribution_format: null,
                selectedForImport: true
              }),
              new Resource({
                id: "cce176b3-8247-4771-9333-8e4b514ddd0a",
                resource_type: null,
                license_type: null,
                last_modified: null,
                created: "2020-05-07T22:44:11.232778",
                name: null,
                format: "JSON",
                url: "http://datahub.io/index.json",
                description: "Index of the novel",
                state: null,
                size: null,
                distribution_format: null,
                selectedForImport: true
              })
            ],
            notes: "Some test notes\n\n### A 3rd level heading\n\n**Some bolded text.**\n\n*Some italicized text.*\n\nForeign characters:\nu with umlaut ü\n66-style quote “\nforeign word: thümb\n\nNeeds escaping:\nleft arrow <\n\n<http://ckan.net/>\n\n",
            url: "http://datahub.io",
            license_title: "Other (Open)",
            author: null,
            type: "dataset",
            isShowMore: false
          })
        ],
        filters: {
          "groups": [
            new Filter({
              type: "groups",
              name: "roger",
              display_name: "Roger's books",
              count: 1
            }),
            new Filter({
              type: "groups",
              name: "david",
              display_name: "Dave's books",
              count: 2
            })
          ],
          "organization": [
            new Filter({
              type: "organization",
              name: "knowdive_test",
              display_name: "knowdive_test",
              count: 1
            })
          ],
          "resource_license_en": [],
          "res_format": [
            new Filter({
              type: "res_format",
              name: "plain text",
              display_name: "plain text",
              count: 1
            }),
            new Filter({
              type: "res_format",
              name: "JSON",
              display_name: "JSON",
              count: 1
            }),
            new Filter({
              type: "res_format",
              name: "CSV",
              display_name: "CSV",
              count: 1
            })
          ],
          "tags": [
            new Filter({
              type: "tags",
              name: "tolstoy",
              display_name: "tolstoy",
              count: 1
            }),
            new Filter({
              type: "tags",
              name: "russian",
              display_name: "russian",
              count: 2
            }),
            new Filter({
              type: "tags",
              name: "medical",
              display_name: "medical",
              count: 1
            }),
            new Filter({
              type: "tags",
              name: "Flexible ",
              display_name: "Flexible ",
              count: 2
            })
          ]
        }
      });

      service.getDataSetsAndFilters(request).subscribe(value => {
        expect(value).toEqual(methodResponse);
      });

      // Expect a call to this URL
      const req = httpTestingController.expectOne(
        'http://example.com' + encodeURIComponent('/api/3/action/package_search'
        + '?facet.field=["tags","organization","groups","res_format","resource_license_en"]')
      );
      // Respond with this data when called
      req.flush(serverResponse);
      // Call tick which actually processes te response
      tick();

    })
  );

  /**
   * [x] getDataSetsAndFilters() error
   * [x] apiUrl
   * [ ] apiKey
   */
  it(`#getDataSetsAndFilters (error)`,
    fakeAsync(() => {

      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: ''
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);

      const request = new DatasetsSearchRequest();

      // testing method
      let response: any;
      let errResponse: any = '';
      const mockErrorResponse = {status: 400, statusText: 'Bad Request'};
      const serverResponse = 'Something bad happened; please try again later.';
      service.getDataSetsAndFilters(request).subscribe(value => response = value, err => errResponse = err);
      httpTestingController.expectOne('http://example.com' + encodeURIComponent('/api/3/action/package_search'
        + '?facet.field=["tags","organization","groups","res_format","resource_license_en"]')
      ).flush(serverResponse, mockErrorResponse);
      expect(errResponse).toBe(serverResponse);
      // Call tick which actually processes to response
      tick();

    })
  );

  /**
   * [x] getTags() normal
   * [x] apiUrl
   * [ ] apiKey
   */
  it(`#getTags should return top 10 most used tags (normal)`,
    fakeAsync(() => {

      const stubConfig: { [key: string]: string } = {
        apiUrl: 'http://example.com',
        apiKey: ''
      }
      // mock the api key and url values.
      appConfigServiceSpy.config.and.returnValue(stubConfig);

      const request = new TopTagsRequest({facet_limit: 10});
      const serverResponse = {
        "help": "http://example.com/api/3/action/help_show?name=package_search",
        "success": true,
        "result": {
          "count": 3,
          "sort": "score desc, metadata_modified desc",
          "facets": {
            "tags": {
              "russian": 2,
              "Flexible ": 2,
              "tolstoy": 1,
              "medical": 1
            }
          },
          "results": [],
          "search_facets": {
            "tags": {
              "items": [
                {
                  "count": 1,
                  "display_name": "tolstoy",
                  "name": "tolstoy"
                },
                {
                  "count": 2,
                  "display_name": "russian",
                  "name": "russian"
                },
                {
                  "count": 1,
                  "display_name": "medical",
                  "name": "medical"
                },
                {
                  "count": 2,
                  "display_name": "Flexible ",
                  "name": "Flexible "
                }
              ],
              "title": "tags"
            }
          }
        }
      };
      const methodResponse = new Dictionary({
        count: 3,
        filters: {
          "tags": [
            new Filter({
              type: "tags",
              name: "tolstoy",
              display_name: "tolstoy",
              count: 1
            }),
            new Filter({
              type: "tags",
              name: "russian",
              display_name: "russian",
              count: 2
            }),
            new Filter({
              type: "tags",
              name: "medical",
              display_name: "medical",
              count: 1
            }),
            new Filter({
              type: "tags",
              name: "Flexible ",
              display_name: "Flexible ",
              count: 2
            })
          ]
        }
      });

      service.getTags(request).subscribe(value => {
        expect(value).toEqual(methodResponse);
      });

      // Expect a call to this URL
      const req = httpTestingController.expectOne(
        'http://example.com' + encodeURIComponent('/api/3/action/package_search?facet.field=["tags"]&facet.limit=10&rows=0')
      );
      // Respond with this data when called
      req.flush(serverResponse);
      // Call tick which actually processes to response
      tick();

    })
  );

});
