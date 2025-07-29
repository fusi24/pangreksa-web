package com.fusi24.pangreksa.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.*;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@Slf4j
@SpringBootTest
public class PersonTest {

    @Autowired
    private PersonService personService;

    @Autowired
    private FwAppUserRepository fwAppUserRepository;

    private FwAppUser user;

    @BeforeEach
    public void getUser(){
        this.user =  fwAppUserRepository.findByUsername("medisa").orElse(new FwAppUser());
    }

    private HrPerson getDummyData(){
        try {
            // 1. Fetch JSON from randomuser.me
            URL url = new URL("https://randomuser.me/api/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream is = conn.getInputStream();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode user = root.get("results").get(0);

            // 2. Map JSON to HrPerson
            HrPerson hrPerson = new HrPerson();
            hrPerson.setFirstName(user.get("name").get("first").asText());
            hrPerson.setMiddleName(""); // randomuser.me does not provide middle name
            hrPerson.setLastName(user.get("name").get("last").asText());
            hrPerson.setGender(
                "male".equalsIgnoreCase(user.get("gender").asText()) ? GenderEnum.MALE : GenderEnum.FEMALE
            );
            hrPerson.setNationality(NationalityEnum.OTHER);
            hrPerson.setReligion(ReligionEnum.HINDU);
            hrPerson.setMarriage(MarriageEnum.YES);
            hrPerson.setDob(LocalDate.parse(
                user.get("dob").get("date").asText().substring(0, 10),
                DateTimeFormatter.ISO_LOCAL_DATE
            ));
            hrPerson.setPob(user.get("location").get("city").asText());
            hrPerson.setKtpNumber(user.get("id").get("value").asText());

            log.debug("Fetched HrPerson: {} {}", hrPerson.getFirstName(), hrPerson.getLastName());

            // Other fields can be set to null or default as needed

//            // 3. Save person
//            personService.workingWithPerson(hrPerson);
//            personService.savePerson();
            return hrPerson;
        } catch (Exception e) {
            log.error("Failed to get dummy data", e);
        }

        return null;
    }

    @Test
    void workingWithPerson() {
        HrPerson hrPerson = getDummyData();
        assertThat(hrPerson).isNotNull();

        //set person in service
        personService.workingWithPerson(hrPerson, this.user);

        try {
            // save Person
            personService.savePerson();
            assertThat(hrPerson.getId()).isNotNull();

            // Save Contact
            HrPersonContact contact = new HrPersonContact();
            contact.setDesignation("John Doe");
            contact.setType(ContactTypeEnum.EMAIL);
            contact.setStringValue("info@fusi24.com");

            List<HrPersonContact> contactList = List.of(contact);
            personService.saveContact(contactList);

            // Save Address
            HrPersonAddress address = new HrPersonAddress();
            address.setFullAddress("123 Main St, City, Country");
            address.setIsDefault(false);

            List<HrPersonAddress> addressList = List.of(address);
            personService.saveAddress(addressList);

            // Save Education
            HrPersonEducation education = new HrPersonEducation();
            education.setInstitution("University of Indonesia");
            education.setProgram("Magister Management");
            education.setScore(new BigDecimal(3.4));
            education.setStartDate(LocalDate.of(2015, 1, 1));
            education.setFinishDate(LocalDate.of(2017, 1, 1));
            education.setType(EducationTypeEnum.ACADEMIC);

            List<HrPersonEducation> educationList = List.of(education);
            personService.saveEducation(educationList);



        } catch (IllegalStateException ise) {
            log.error("Error working with person: {}", ise.getMessage());
            assertThat(ise).isNull(); // raise error
        }
    }


    @Test
    public void retrievePersonFullData(){
        Long personId = 25L; // Example person ID

        HrPerson person = personService.getPerson(personId);
        log.info("Person: {}", person);
        assertThat(person).isNotNull();

        List<HrPersonContact> contacts = personService.getPersonContacts();
        log.info("Contacts:");
        contacts.forEach(contact -> log.info(" - {}", contact));
        assertThat(contacts).isNotNull();
        assertThat(contacts).isNotEmpty();

        List<HrPersonAddress> addresses = personService.getPersonAddresses();
        log.info("Addresses:");
        addresses.forEach(address -> log.info(" - {}", address));
        assertThat(addresses).isNotNull();
        assertThat(addresses).isNotEmpty();

        List<HrPersonEducation> educations1 = personService.getPersonEducations();
        log.info("Educations (1st call):");
        educations1.forEach(education -> log.info(" - {}", education));
        assertThat(educations1).isNotNull();
        assertThat(educations1).isNotEmpty();

        List<HrPersonEducation> educations2 = personService.getPersonEducations();
        log.info("Educations (2nd call):");
        educations2.forEach(education -> log.info(" - {}", education));
        assertThat(educations2).isNotNull();
        assertThat(educations2).isNotEmpty();
    }
}
