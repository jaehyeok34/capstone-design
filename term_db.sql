-- MySQL dump 10.13  Distrib 9.3.0, for macos15.2 (arm64)
--
-- Host: localhost    Database: term_db
-- ------------------------------------------------------
-- Server version	9.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `term_db`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `term_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `term_db`;

--
-- Table structure for table `standard_term_info`
--

DROP TABLE IF EXISTS `standard_term_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `standard_term_info` (
  `standard_term` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `synonym_group_id` int NOT NULL,
  `category` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_sensitive` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`standard_term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `standard_term_info`
--

LOCK TABLES `standard_term_info` WRITE;
/*!40000 ALTER TABLE `standard_term_info` DISABLE KEYS */;
INSERT INTO `standard_term_info` VALUES ('생년월일',5,'식별정보',1),('성별',7,'식별정보',1),('연락처',6,'식별정보',1),('이름',2,'식별정보',1),('전화번호',3,'식별정보',1),('주민번호',1,'식별정보',1),('주소',4,'식별정보',1);
/*!40000 ALTER TABLE `standard_term_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `term_mapping`
--

DROP TABLE IF EXISTS `term_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `term_mapping` (
  `term` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `standard_term` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`term`),
  KEY `standard_term` (`standard_term`),
  CONSTRAINT `term_mapping_ibfk_1` FOREIGN KEY (`standard_term`) REFERENCES `standard_term_info` (`standard_term`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `term_mapping`
--

LOCK TABLES `term_mapping` WRITE;
/*!40000 ALTER TABLE `term_mapping` DISABLE KEYS */;
INSERT INTO `term_mapping` VALUES ('birth','생년월일'),('birth_date','생년월일'),('birthyear','생년월일'),('date_of_birth','생년월일'),('생년월일','생년월일'),('fullname','이름'),('name','이름'),('고객명','이름'),('성명','이름'),('성함','이름'),('이름','이름'),('mobilecarrier','전화번호'),('phone','전화번호'),('phone_number','전화번호'),('phonemodel','전화번호'),('phonenumber','전화번호'),('폰번호','전화번호'),('휴대번호','전화번호'),('휴대전화','전화번호'),('휴대전화번호','전화번호'),('idnumber','주민번호'),('resident_registration_number','주민번호'),('ssn','주민번호'),('주민등록번호','주민번호'),('주민번호','주민번호'),('address','주소'),('location','주소'),('residence','주소'),('거주지','주소'),('주거중인곳','주소');
/*!40000 ALTER TABLE `term_mapping` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-06-06 18:20:54
