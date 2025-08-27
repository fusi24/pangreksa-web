package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.*;
import com.fusi24.pangreksa.web.view.employee.ProfilDataKaryawanView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PersonService {
    private static final Logger log = LoggerFactory.getLogger(PersonService.class);

    private final HrPersonRespository hrPersonRespository;
    private final HrPersonContactRepository hrPersonContactRepository;
    private final HrPersonAddressRepository hrPersonAddressRepository;
    private final HrPersonEducationRepository hrPersonEducationRepository;
    private final HrPersonDocumentRepository hrPersonDocumentRepository;
    private final FwAppUserRepository appUserRepository;
    private final FwSystemRepository systemRepository;

    private final HrPersonPositionRepository hrPersonPositionRepository;
    private final HrCompanyRepository hrCompanyRepository;

    private HrPerson hrPerson = null;
    private FwAppUser user = null;
    private HrCompany hrCompany = null;

    private static final int NO_RETRIEVE = 10;

    private String PERSON_PHOTO_PATH;

    public PersonService(HrPersonRespository personRepository,
                         HrPersonContactRepository contactRepository,
                         HrPersonAddressRepository addressRepository,
                         HrPersonEducationRepository educationRepository,
                         HrPersonDocumentRepository documentRepository,
                         HrPersonPositionRepository hrPersonPositionRepository,
                         HrCompanyRepository hrCompanyRepository,
                         FwAppUserRepository appUserRepository,
                         FwSystemRepository systemRepository) {
        this.hrPersonRespository = personRepository;
        this.hrPersonContactRepository = contactRepository;
        this.hrPersonAddressRepository = addressRepository;
        this.hrPersonEducationRepository = educationRepository;
        this.hrPersonDocumentRepository = documentRepository;
        this.hrPersonPositionRepository = hrPersonPositionRepository;
        this.hrCompanyRepository = hrCompanyRepository;
        this.appUserRepository = appUserRepository;
        this.systemRepository =  systemRepository;

        PERSON_PHOTO_PATH = systemRepository.findById(UUID.fromString("a4b91eca-9367-4b90-8ac2-71115817056f")).orElseThrow().getStringVal();
    }

    public byte[] getPhotoAsByteArray(String filename) {
        // check the file exists in the physical path, and return byte array
        String filePath = PERSON_PHOTO_PATH + File.separator + filename;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read photo file: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalStateException("File not found: " + filePath);
        }
    }

    public String getPersonPhotoPath(String filename) {
        // check the file exists in the physical path, and return full path with PERSON_PHOTO_PATH
        String filePath = PERSON_PHOTO_PATH + File.separator + filename;
        File file = new File(filePath);
        if (file.exists()) {
            return filePath;
        } else {
            throw new IllegalStateException("File not found: " + filePath);
        }
    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public Boolean deletePhoto(String filename) {
        // delete from physical path
        String filePath = PERSON_PHOTO_PATH + File.separator + filename;
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        } else {
            throw new IllegalStateException("File not found: " + filePath);
        }
    }

    public Boolean uploadPhoto(byte[] uploadedImageBytes, String filename) {
        // upload to physical path
        String filePath = PERSON_PHOTO_PATH + File.separator + filename;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(uploadedImageBytes)) {
            File file = new File(filePath);
            // Use REPLACE_EXISTING to overwrite if file already exists
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                return file.createNewFile();
            }

            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload photo: " + e.getMessage(), e);
        }
    }

    public void workingWithPerson(HrPerson person, FwAppUser user) {
        this.user = user;

        person.setCreatedBy( person.getCreatedBy() != null ? person.getCreatedBy() : user);
        person.setUpdatedBy(user);
        this.hrPerson = person;
    }

    public void savePerson() {
        if (hrPerson != null) {

            hrPerson.setCreatedBy( hrPerson.getCreatedBy() != null ? hrPerson.getCreatedBy() : user);
            hrPerson.setUpdatedBy(user);
            this.hrPerson = hrPersonRespository.save(hrPerson);
        } else {
            throw new IllegalStateException("No person to save");
        }
    }

    public void saveContact(List<HrPersonContact> contactList) {
        if (hrPerson != null) {
            for (HrPersonContact contact : contactList) {

                contact.setCreatedBy( contact.getCreatedBy() != null ? contact.getCreatedBy() : user);
                contact.setUpdatedBy(user);

                contact.setPerson(hrPerson);
                hrPersonContactRepository.save(contact);
            }
        } else {
            throw new IllegalStateException("No person to save contact for");
        }
    }

    public void saveAddress(List<HrPersonAddress> addressList) {
        if (hrPerson != null) {
            for (HrPersonAddress address : addressList) {
                address.setCreatedBy( address.getCreatedBy() != null ? address.getCreatedBy() : user);
                address.setUpdatedBy(user);

                address.setPerson(hrPerson);
                hrPersonAddressRepository.save(address);
            }
        } else {
            throw new IllegalStateException("No person to save contact for");
        }
    }

    public void saveEducation(List<HrPersonEducation> educationList) {
        if (hrPerson != null) {
            for (HrPersonEducation education : educationList) {
                education.setCreatedBy( education.getCreatedBy() != null ? education.getCreatedBy() : user);
                education.setUpdatedBy(user);
                education.setPerson(hrPerson);
                hrPersonEducationRepository.save(education);
            }
        } else {
            throw new IllegalStateException("No person to save education for");
        }
    }

    public void saveDocument(List<HrPersonDocument> documentList) {
        if (hrPerson != null) {
            for (HrPersonDocument document : documentList) {
                document.setCreatedBy( document.getCreatedBy() != null ? document.getCreatedBy() : user);
                document.setUpdatedBy(user);
                document.setPerson(hrPerson);
                hrPersonDocumentRepository.save(document);
            }
        } else {
            throw new IllegalStateException("No person to save document for");
        }
    }

    public HrPerson getPerson(Long id) {
        HrPerson person = hrPersonRespository.findById(id).orElse(this.hrPerson);
        this.hrPerson = person;
        return person;
    }

    public List<HrPersonContact> getPersonContacts() {
        return hrPersonContactRepository.findByPerson(this.hrPerson);
    }

    public List<HrPersonAddress> getPersonAddresses() {
        return hrPersonAddressRepository.findByPerson(this.hrPerson);
    }

    public List<HrPersonEducation> getPersonEducations() {
        return hrPersonEducationRepository.findByPerson(this.hrPerson);
    }

    public List<HrPersonDocument> getPersonDocuments() {
        return hrPersonDocumentRepository.findByPerson(this.hrPerson);
    }

    public List<HrPerson> findAllPerson() {
        return hrPersonRespository.findAll();
    }

    public List<HrPerson> findUnassignedPersons() {
        return hrPersonRespository.findUnassignedPersons();
    }

//    public List<HrPerson> findallPersonByCompany() {
//        if (hrCompany == null) {
//            throw new IllegalStateException("No company selected");
//        }
//
//        List<HrPerson> personList =  hrPersonRespository.findAll();
//
//        return personList.stream()
//                .filter(person -> person.getUpdatedBy().getPerson().))
//                .toList();
//    }

    public void workingWithCompany(HrCompany company) {
        if (company == null) {
            throw new IllegalStateException("Company cannot be null");
        }
        this.hrCompany = company;
    }

    public HrCompany getCompanyById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        this.hrCompany = hrCompanyRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Company not found with ID: " + id));
        return this.hrCompany;
    }

    public List<HrPersonPosition> getPersonHasPositionInCompany() {
        if (hrCompany == null) {
            throw new IllegalStateException("No company selected");
        }

        List<HrPersonPosition> positions = hrPersonPositionRepository.findByCompany(this.hrCompany);

        return positions; // Assuming you want the first position, adjust as needed
    }

    public List<HrPersonPosition> getPersonHasPositionInCompanyByOrgStructure(HrCompany company, HrOrgStructure orgStructure) {
        List<HrPersonPosition> positions = hrPersonPositionRepository.findByCompanyAndPosition_OrgStructure(company, orgStructure);

        return positions; // Assuming you want the first position, adjust as needed
    }

    public List<HrPersonPosition> getPersonHasPositionInCompanyByPosition(HrCompany company, HrPosition position) {
        List<HrPersonPosition> positions = this.getPersonHasPositionInCompanyByOrgStructure(company, position.getOrgStructure());
        log.debug("Found {} person. try to filter with orgStructure {}", positions.size(), position.getOrgStructure().getName());

        return positions
                .stream()
                .filter(p -> p.getPosition().getId().equals(position.getId()))
                .toList(); // Java 16+ (or use .collect(Collectors.toList()) for older versions)
    }

    public List<HrPersonPosition> getPersonHasPositionInCompanyByKeyword(HrCompany company, String keyword, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<HrPersonPosition> positions = hrPersonPositionRepository.searchByPersonKeyword(company, keyword, pageable);

        return positions; // Assuming you want the first position, adjust as needed
    }

    public void saveAllInformation(List<HrPersonAddress> addresses,
                                       List<HrPersonContact> contacts,
                                       List<HrPersonEducation> educations,
                                       List<HrPersonDocument> documents) {
        this.hrPerson = hrPersonRespository.save(hrPerson);

        for( HrPersonAddress address : addresses) {
            address.setPerson(hrPerson);
            address.setCreatedBy(address.getCreatedBy() != null ? address.getCreatedBy() : user);
            address.setCreatedAt(address.getCreatedAt() != null ? address.getCreatedAt() : LocalDateTime.now());
            address.setUpdatedBy(user);
            address.setUpdatedAt(LocalDateTime.now());
            hrPersonAddressRepository.save(address);
        }

        for( HrPersonContact contact : contacts) {
            contact.setPerson(hrPerson);
            contact.setCreatedBy(contact.getCreatedBy() != null ? contact.getCreatedBy() : user);
            contact.setCreatedAt(contact.getCreatedAt() != null ? contact.getCreatedAt() : LocalDateTime.now());
            contact.setUpdatedBy(user);
            contact.setUpdatedAt(LocalDateTime.now());
            hrPersonContactRepository.save(contact);
        }

        for( HrPersonEducation education : educations) {
            education.setPerson(hrPerson);
            education.setCreatedBy(education.getCreatedBy() != null ? education.getCreatedBy() : user);
            education.setCreatedAt(education.getCreatedAt() != null ? education.getCreatedAt() : LocalDateTime.now());
            education.setUpdatedBy(user);
            education.setUpdatedAt(LocalDateTime.now());
            hrPersonEducationRepository.save(education);
        }

        for( HrPersonDocument document : documents) {
            document.setPerson(hrPerson);
            document.setCreatedBy(document.getCreatedBy() != null ? document.getCreatedBy() : user);
            document.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt() : LocalDateTime.now());
            document.setUpdatedBy(user);
            document.setUpdatedAt(LocalDateTime.now());
            hrPersonDocumentRepository.save(document);
        }

    }

    public void deleteAddress(HrPersonAddress address) {
        if (address == null || address.getId() == null) {
            throw new IllegalArgumentException("Address cannot be null or have a null ID");
        }
        hrPersonAddressRepository.deleteById(address.getId());
    };

    public void deleteContact(HrPersonContact contact) {
        if (contact == null || contact.getId() == null) {
            throw new IllegalArgumentException("Contact cannot be null or have a null ID");
        }
        hrPersonContactRepository.deleteById(contact.getId());
    };

    public void deleteEducation(HrPersonEducation education) {
        if (education == null || education.getId() == null) {
            throw new IllegalArgumentException("Education cannot be null or have a null ID");
        }
        hrPersonEducationRepository.deleteById(education.getId());
    };

    public void deleteDocument(HrPersonDocument document) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Document cannot be null or have a null ID");
        }
        hrPersonDocumentRepository.deleteById(document.getId());
    };

    public List<HrPerson> findPersonByKeyword(String keyword) {
        Pageable pageable = PageRequest.of(0, NO_RETRIEVE);
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<HrPerson>();
        }
        return hrPersonRespository.findByKeyword(keyword, pageable);
    }

    public List<HrCompany> findCompanyByKeyword(String keyword) {
        Pageable pageable = PageRequest.of(0, NO_RETRIEVE);
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<HrCompany>();
        }
        return hrCompanyRepository.findByKeyword(keyword, pageable);
    }

    public HrPersonPosition savePersonPosition(HrPersonPosition personPosition, AppUserInfo appUserInfo) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if(personPosition.getId() == null) {
            personPosition.setCreatedBy(appUser);
            personPosition.setUpdatedBy(appUser);
        } else {
            personPosition.setUpdatedBy(appUser);
        }

        return hrPersonPositionRepository.save(personPosition);
    }

    public HrPerson getManager(AppUserInfo appUserInfo) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());
        HrCompany company = appUser.getCompany();
        HrPerson person = appUser.getPerson();
        HrPersonPosition personPosition = hrPersonPositionRepository.findCurrentPositionsByCompanyAndPerson(company, person, LocalDate.now());
        if (personPosition == null) {
            return null;
        }
        HrPosition managerPosition = personPosition.getPosition().getReportsTo();
        if (managerPosition == null) {
            return null;
        }
        // find manger that has position in the same company
        List<HrPersonPosition> managerPositionsList = hrPersonPositionRepository.findByCompanyAndPosition(company, managerPosition);
        if (managerPositionsList == null) {
            return null;
        }
        return managerPositionsList.get(0).getPerson();
    }
}
