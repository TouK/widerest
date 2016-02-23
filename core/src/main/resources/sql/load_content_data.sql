--
-- The Archetype is configured with "hibernate.hbm2ddl.auto" value="create-drop" in "persistence.xml".
--
-- This will cause hibernate to populate the database when the application is started by processing the files that
-- were configured in the hibernate.hbm2ddl.import_files property.
--
-- This file loads some sample content pages and structured content data.
--

-----------------------------------------------------------------------------------------------------------------------------------
-- SAMPLE PAGE DATA - would typically be entered via the admin
-----------------------------------------------------------------------------------------------------------------------------------


-- Create an about-us page with "test-content" as the body of the page.
INSERT INTO BLC_PAGE (PAGE_ID, DESCRIPTION, PAGE_TMPLT_ID, FULL_URL) VALUES (1, 'About Us', 1, '/about_us');
INSERT INTO BLC_PAGE (PAGE_ID, DESCRIPTION, PAGE_TMPLT_ID, FULL_URL) VALUES (2, 'FAQ', 1, '/faq');
INSERT INTO BLC_PAGE (PAGE_ID, DESCRIPTION, PAGE_TMPLT_ID, FULL_URL) VALUES (3, 'New to Hot Sauce', 1, '/new-to-hot-sauce');

INSERT INTO BLC_PAGE_FLD(PAGE_FLD_ID, FLD_KEY, VALUE, PAGE_ID) VALUES (1, 'body', 'test content', 1);
INSERT INTO BLC_PAGE_FLD(PAGE_FLD_ID, FLD_KEY, VALUE, PAGE_ID) VALUES (2, 'title', 'test title', 1);

INSERT INTO BLC_PAGE_FLD(PAGE_FLD_ID, FLD_KEY, VALUE, PAGE_ID) VALUES (3, 'body', '<h2 style="text-align:center;">This is an example of a content-managed page.</h2><h4 style="text-align:center;"><a href="http://www.broadleafcommerce.com/features/content">Click Here</a> to see more about Content Management in Broadleaf.</h4>', 2);

-- This creates an empty new to hot sauce 
INSERT INTO BLC_PAGE_FLD(PAGE_FLD_ID, FLD_KEY, VALUE, PAGE_ID) VALUES (4, 'body', '<h2 style="text-align:center;">This is an example of a content-managed page.</h2>', 3);

-----------------------------------------------------------------------------------------------------------------------------------
-- SAMPLE STRUCTURED CONTENT DATA  - would typically be entered via the admin
-----------------------------------------------------------------------------------------------------------------------------------

---------------------------------------------------
-- HOME PAGE BANNER
---------------------------------------------------
-- Content Item
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Buy One Get One - Twice the Burn', FALSE, 5, 'en', 1);
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (101, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Shirt Special - 20% off all shirts', FALSE, 1, 'en', 1);
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (102, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Member Special - $10 off next order over $50', FALSE, 5, 'en', 1);

-- Fields
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (1, CURRENT_TIMESTAMP, 'imageUrl', 1, '/img/banners/buy-one-get-one-home-banner.jpg');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (2, CURRENT_TIMESTAMP, 'targetUrl', 1, '/hot-sauces');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (3, CURRENT_TIMESTAMP, 'imageUrl', 1, '/img/banners/shirt-special.jpg');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (4, CURRENT_TIMESTAMP, 'targetUrl', 1, '/merchandise');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (5, CURRENT_TIMESTAMP, 'imageUrl', 1, '/img/banners/member-special.jpg');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (6, CURRENT_TIMESTAMP, 'targetUrl', 1, '/register');

-- Field XREF
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-158, 100, 1, 'imageUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-159, 100, 2, 'targetUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-160, 101, 3, 'imageUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-161, 101, 4, 'targetUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-162, 102, 5, 'imageUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-163, 102, 6, 'targetUrl');

---------------------------------------------------
-- HOME PAGE SNIPIT
---------------------------------------------------
-- Content Item
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (110, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Home Page Snippet - Aficionado', FALSE, 5, 'en', 2);

-- Fields
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (9, CURRENT_TIMESTAMP, 'htmlContent', 1, '<h2>HOT SAUCE AFICIONADO?</h2> Click to join our Heat Clinic Frequent Care Program. The place to get all the deals on burn treatment.');

-- Field XREF
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-164, 110, 9, 'htmlContent');

---------------------------------------------------
-- HOME PAGE FEATURED PRODUCTS MESSAGE
---------------------------------------------------
-- Content Item
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (130, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Home Page Featured Products Title', FALSE, 5, 'en', 3);

-- Fields
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (11, CURRENT_TIMESTAMP, 'messageText', 1, 'The Heat Clinic''s Top Selling Sauces');

-- Field XREF
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-165, 130, 11, 'messageText');



---------------------------------------------------
-- RIGHT HAND SIDE - AD
---------------------------------------------------
-- Content Item
INSERT INTO BLC_SC (SC_ID, CREATED_BY, DATE_CREATED, DATE_UPDATED, UPDATED_BY, CONTENT_NAME, OFFLINE_FLAG, PRIORITY, LOCALE_CODE, SC_TYPE_ID) VALUES (140, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'RHS - The Essentials Collection', FALSE, 5, 'en', 4);

-- Fields
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (12, CURRENT_TIMESTAMP, 'imageUrl', 1, '/img/rhs-ad.jpg');
INSERT INTO BLC_SC_FLD (SC_FLD_ID, DATE_CREATED, FLD_KEY, CREATED_BY, VALUE) VALUES (13, CURRENT_TIMESTAMP, 'targetUrl', 1, '/hot-sauces');

-- Field XREF
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-166, 140, 12, 'imageUrl');
INSERT INTO BLC_SC_FLD_MAP (BLC_SC_SC_FIELD_ID, SC_ID, SC_FLD_ID, MAP_KEY) VALUES (-167, 140, 13, 'targetUrl');
