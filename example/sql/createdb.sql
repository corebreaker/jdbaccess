-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema example
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `example` ;

-- -----------------------------------------------------
-- Schema example
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `example` DEFAULT CHARACTER SET utf8 ;
USE `example` ;

-- -----------------------------------------------------
-- Table `person`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `person` ;

CREATE TABLE IF NOT EXISTS `person` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `birth` DATE NOT NULL,
  `ref_manager` BIGINT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_person_person`
    FOREIGN KEY (`ref_manager`)
    REFERENCES `person` (`id`)
    ON DELETE SET NULL
    ON UPDATE SET NULL)
ENGINE = InnoDB;

CREATE INDEX `fk_person_person_idx` ON `person` (`ref_manager` ASC);

SET SQL_MODE = '';
GRANT USAGE ON *.* TO example;
 DROP USER example;
SET SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';
CREATE USER 'example' IDENTIFIED BY 'example';

GRANT ALL ON example.* TO 'example';

-- -----------------------------------------------------
-- Data for table `person`
-- -----------------------------------------------------
START TRANSACTION;
USE `example`;
INSERT INTO `person` (`id`, `name`, `birth`, `ref_manager`) VALUES (1, 'helen', '1980-06-15', NULL);
INSERT INTO `person` (`id`, `name`, `birth`, `ref_manager`) VALUES (2, 'jack', '1986-02-10', 1);

COMMIT;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
