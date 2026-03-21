--
-- PostgreSQL database dump
--


-- Dumped from database version 18.2
-- Dumped by pg_dump version 18.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_data (
    id uuid NOT NULL,
    auth_provider character varying(255) NOT NULL,
    email character varying(255),
    password character varying(255),
    provider_id character varying(255),
    registered_at timestamp(6) with time zone NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT auth_data_auth_provider_check CHECK (((auth_provider)::text = ANY ((ARRAY['LOCAL'::character varying, 'VK'::character varying])::text[]))),
    CONSTRAINT auth_data_role_check CHECK (((role)::text = ANY ((ARRAY['GUEST'::character varying, 'STUDENT'::character varying, 'TEACHER'::character varying, 'ADMIN'::character varying])::text[])))
);


--
-- Name: course_editors_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_editors_mapping (
    course_id bigint NOT NULL,
    editor_id uuid NOT NULL
);


--
-- Name: course_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.course_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: course_tag_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.course_tag_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: course_tags_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.course_tags_mapping (
    course_id bigint NOT NULL,
    tag_id bigint NOT NULL
);


--
-- Name: courses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.courses (
    id bigint NOT NULL,
    description character varying(255),
    handle character varying(255) NOT NULL,
    max_users_amount integer NOT NULL,
    name character varying(255) NOT NULL,
    owner_id uuid NOT NULL,
    preview_id uuid
);


--
-- Name: enrollment_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.enrollment_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: enrollments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.enrollments (
    id bigint NOT NULL,
    enroll_since timestamp(6) with time zone NOT NULL,
    course_id bigint NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: file_attachment_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.file_attachment_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: file_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file_attachments (
    id bigint NOT NULL,
    file_visibility character varying(255) NOT NULL,
    purpose character varying(255) NOT NULL,
    resource_id character varying(255) NOT NULL,
    resource_type character varying(255) NOT NULL,
    file_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    CONSTRAINT file_attachments_file_visibility_check CHECK (((file_visibility)::text = ANY ((ARRAY['PRIVATE'::character varying, 'ENROLLED'::character varying, 'PUBLIC'::character varying])::text[]))),
    CONSTRAINT file_attachments_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['AVATAR'::character varying, 'PREVIEW'::character varying, 'MATERIAL'::character varying])::text[]))),
    CONSTRAINT file_attachments_resource_type_check CHECK (((resource_type)::text = ANY ((ARRAY['PROFILE'::character varying, 'COURSE'::character varying, 'LESSON'::character varying])::text[])))
);


--
-- Name: files; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.files (
    id uuid NOT NULL,
    content_type character varying(255) NOT NULL,
    original_filename character varying(255) NOT NULL,
    storage_path character varying(255) NOT NULL,
    uploaded_at timestamp(6) with time zone NOT NULL
);


--
-- Name: lesson; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lesson (
    id bigint NOT NULL,
    begin_at timestamp(6) with time zone NOT NULL,
    description character varying(255),
    ends_at timestamp(6) with time zone NOT NULL,
    name character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    course_id bigint NOT NULL,
    owner_id uuid NOT NULL,
    CONSTRAINT lesson_state_check CHECK (((state)::text = ANY ((ARRAY['PLANNED'::character varying, 'ONGOING'::character varying, 'CANCELLED'::character varying, 'FINISHED'::character varying])::text[]))),
    CONSTRAINT lesson_type_check CHECK (((type)::text = ANY ((ARRAY['LECTURE'::character varying, 'PRACTICE'::character varying])::text[])))
);


--
-- Name: lesson_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lesson_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_token_sequence; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_token_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    expire_date timestamp(6) with time zone NOT NULL,
    token character varying(255) NOT NULL,
    valid boolean NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tags (
    id bigint NOT NULL,
    tag_name character varying(255) NOT NULL
);


--
-- Name: user_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_profile (
    id uuid NOT NULL,
    full_name character varying(255) NOT NULL,
    handle character varying(255) NOT NULL,
    summary character varying(500),
    avatar_id uuid,
    user_id uuid
);


--
-- Name: auth_data auth_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_data
    ADD CONSTRAINT auth_data_pkey PRIMARY KEY (id);


--
-- Name: course_editors_mapping course_editors_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_editors_mapping
    ADD CONSTRAINT course_editors_mapping_pkey PRIMARY KEY (course_id, editor_id);


--
-- Name: course_tags_mapping course_tags_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_tags_mapping
    ADD CONSTRAINT course_tags_mapping_pkey PRIMARY KEY (course_id, tag_id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: enrollments enrollments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);


--
-- Name: file_attachments file_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT file_attachments_pkey PRIMARY KEY (id);


--
-- Name: files files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.files
    ADD CONSTRAINT files_pkey PRIMARY KEY (id);


--
-- Name: lesson lesson_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson
    ADD CONSTRAINT lesson_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: tags tags_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);


--
-- Name: user_profile uk1rt110k0ciuitln2fr5p9y7x3; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT uk1rt110k0ciuitln2fr5p9y7x3 UNIQUE (avatar_id);


--
-- Name: tags uk2c6s9hekidseaj5vbgb3pgy3k; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tags
    ADD CONSTRAINT uk2c6s9hekidseaj5vbgb3pgy3k UNIQUE (tag_name);


--
-- Name: courses uk5o6x4fpafbywj4v2g0owhh11r; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT uk5o6x4fpafbywj4v2g0owhh11r UNIQUE (name);


--
-- Name: file_attachments ukb351krqfj8jkvpn55u5d93eum; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT ukb351krqfj8jkvpn55u5d93eum UNIQUE (resource_type, purpose, file_id, resource_id);


--
-- Name: auth_data ukdq2yqfb67u94nekdwhtlepilf; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_data
    ADD CONSTRAINT ukdq2yqfb67u94nekdwhtlepilf UNIQUE (email);


--
-- Name: user_profile ukebc21hy5j7scdvcjt0jy6xxrv; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT ukebc21hy5j7scdvcjt0jy6xxrv UNIQUE (user_id);


--
-- Name: enrollments ukg1muiskd02x66lpy6fqcj6b9q; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT ukg1muiskd02x66lpy6fqcj6b9q UNIQUE (user_id, course_id);


--
-- Name: refresh_tokens ukghpmfn23vmxfu3spu3lfg4r2d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT ukghpmfn23vmxfu3spu3lfg4r2d UNIQUE (token);


--
-- Name: courses ukhlxy0mfs25o8i4ycfh329cwfo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT ukhlxy0mfs25o8i4ycfh329cwfo UNIQUE (handle);


--
-- Name: courses uklbgwgr2aykfafrx1waoowyt7d; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT uklbgwgr2aykfafrx1waoowyt7d UNIQUE (preview_id);


--
-- Name: user_profile ukqo1lgp8aqn93w2empykx0qq5h; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT ukqo1lgp8aqn93w2empykx0qq5h UNIQUE (handle);


--
-- Name: auth_data ukr0ilauhn55ll0eg9pbj20i8ly; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_data
    ADD CONSTRAINT ukr0ilauhn55ll0eg9pbj20i8ly UNIQUE (provider_id);


--
-- Name: user_profile user_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT user_profile_pkey PRIMARY KEY (id);


--
-- Name: course_editors_mapping fk2gjrff5ja4pxeqiwl1381o3w6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_editors_mapping
    ADD CONSTRAINT fk2gjrff5ja4pxeqiwl1381o3w6 FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: lesson fk7tt5f230btydmfw5wqd00e78x; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson
    ADD CONSTRAINT fk7tt5f230btydmfw5wqd00e78x FOREIGN KEY (owner_id) REFERENCES public.auth_data(id);


--
-- Name: refresh_tokens fk7vu37jomhecl1g7irf0dypqw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk7vu37jomhecl1g7irf0dypqw FOREIGN KEY (user_id) REFERENCES public.auth_data(id);


--
-- Name: lesson fkbnmu2xq9klrrg69rs7lcn5fku; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lesson
    ADD CONSTRAINT fkbnmu2xq9klrrg69rs7lcn5fku FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: file_attachments fkcl6d7tklc6bjecuuef97kr8i5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT fkcl6d7tklc6bjecuuef97kr8i5 FOREIGN KEY (owner_id) REFERENCES public.auth_data(id);


--
-- Name: courses fkcrscblnji1rxvx0xyc9pk18l9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT fkcrscblnji1rxvx0xyc9pk18l9 FOREIGN KEY (preview_id) REFERENCES public.files(id);


--
-- Name: enrollments fkho8mcicp4196ebpltdn9wl6co; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fkho8mcicp4196ebpltdn9wl6co FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: course_editors_mapping fkin7dacjwjcs8w7hfbq125tcgt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_editors_mapping
    ADD CONSTRAINT fkin7dacjwjcs8w7hfbq125tcgt FOREIGN KEY (editor_id) REFERENCES public.auth_data(id);


--
-- Name: user_profile fkjnnw0hffyd4604krydby271g1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fkjnnw0hffyd4604krydby271g1 FOREIGN KEY (avatar_id) REFERENCES public.files(id);


--
-- Name: file_attachments fkjrevgj9ianlp7tiocpstt90g5; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_attachments
    ADD CONSTRAINT fkjrevgj9ianlp7tiocpstt90g5 FOREIGN KEY (file_id) REFERENCES public.files(id);


--
-- Name: user_profile fkkjwnnh0pj61b8m6oaeebkfrr2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fkkjwnnh0pj61b8m6oaeebkfrr2 FOREIGN KEY (user_id) REFERENCES public.auth_data(id);


--
-- Name: course_tags_mapping fkllo6ewve0o92j5fax9n2xk8gq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_tags_mapping
    ADD CONSTRAINT fkllo6ewve0o92j5fax9n2xk8gq FOREIGN KEY (tag_id) REFERENCES public.tags(id);


--
-- Name: course_tags_mapping fknhtt2gjnnmf08idy3bxad34ob; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.course_tags_mapping
    ADD CONSTRAINT fknhtt2gjnnmf08idy3bxad34ob FOREIGN KEY (course_id) REFERENCES public.courses(id);


--
-- Name: enrollments fkrefxnld36m0cdg47kjfdc7q1a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.enrollments
    ADD CONSTRAINT fkrefxnld36m0cdg47kjfdc7q1a FOREIGN KEY (user_id) REFERENCES public.auth_data(id);


--
-- Name: courses fktgnlf0cuos2ecmcs84p2dn3wi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT fktgnlf0cuos2ecmcs84p2dn3wi FOREIGN KEY (owner_id) REFERENCES public.auth_data(id);


--
-- PostgreSQL database dump complete
--

