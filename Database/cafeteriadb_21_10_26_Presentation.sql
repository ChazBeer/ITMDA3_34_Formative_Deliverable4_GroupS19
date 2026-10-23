-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 21, 2026 at 04:33 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `cafeteriadb`
--

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `category_id` bigint(20) NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `display_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`category_id`, `category_name`, `display_order`, `created_at`, `updated_at`) VALUES
(1, 'Burgers', 2, '2026-07-12 10:32:40', '2026-10-21 13:48:15'),
(3, 'Desserts', 9, '2026-09-28 07:44:07', '2026-10-21 13:48:15'),
(14, 'Breakfast', 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(16, 'Cape Town Favourites', 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(17, 'Wraps & Sandwiches', 4, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(18, 'Rolls & Sides', 5, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(19, 'Hot Meals', 6, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(20, 'Hot Drinks', 7, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(21, 'Cold Drinks', 8, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(23, 'Snacks', 10, '2026-10-21 13:48:15', '2026-10-21 13:48:15');

-- --------------------------------------------------------

--
-- Table structure for table `category_default_groups`
--

CREATE TABLE `category_default_groups` (
  `category_default_group_id` bigint(20) NOT NULL,
  `category_id` bigint(20) NOT NULL,
  `modifier_group_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `menu_items`
--

CREATE TABLE `menu_items` (
  `menu_item_id` bigint(20) NOT NULL,
  `category_id` bigint(20) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `available` tinyint(1) DEFAULT 1,
  `image_path` varchar(255) DEFAULT NULL,
  `display_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `stock_quantity` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `menu_items`
--

INSERT INTO `menu_items` (`menu_item_id`, `category_id`, `name`, `description`, `price`, `available`, `image_path`, `display_order`, `created_at`, `updated_at`, `stock_quantity`) VALUES
(10, 14, 'Breakfast Sandwich', 'Scrambled or sunny-side up eggs and cheese.', 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792483959/edueats/menu-items/40176f61-106f-4f21-b1a4-edb507ed6384.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(11, 14, 'Breakfast Wrap', 'A toasted wrap filled with scrambled egg.', 40.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792163085/edueats/menu-items/0172f1cf-6cda-4e22-b9f9-e9562d3d39a8.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(12, 14, 'Omelette', 'A fluffy egg dish with cheese.', 45.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792484376/edueats/menu-items/aeeee96c-3316-4e9a-8efa-729725c8efc5.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(13, 14, 'Bubble Waffle', 'Bubble waffle with ice cream. Add a topping and syrup for R15.', 40.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792484421/edueats/menu-items/1d217f6b-680a-4823-aed7-cfccbdbf5576.jpg', 4, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(14, 1, 'Classic Beef Burger', NULL, 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792158225/edueats/menu-items/71e1e5ec-dde6-4c35-94a5-617bd2e74e0d.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(15, 1, 'Classic Chicken Burger', NULL, 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792162639/edueats/menu-items/d538ee0b-4c50-4f51-8290-cfbe14111800.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(16, 1, 'Cheesy Mushroom Beef Burger', NULL, 55.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792484589/edueats/menu-items/2a5aedfc-284d-4b2b-a063-ab01a53b5cd9.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(17, 1, 'Masala Steak Burger', NULL, 60.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792490588/edueats/menu-items/092b889e-b29a-432f-930c-e7bde9c6a20d.jpg', 4, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(18, 16, 'Polony Gatsby', 'A full Cape Town gatsby.', 100.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792492277/edueats/menu-items/884ade26-7327-4ec4-a9c6-748f9373ea6b.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(19, 16, 'Chicken Gatsby', 'A full Cape Town gatsby.', 150.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792490363/edueats/menu-items/ab27c360-4749-4fd7-a95a-c4e9fc811665.png', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(20, 17, 'Grilled Chicken Wrap', NULL, 45.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792491072/edueats/menu-items/d1e56ca9-437b-4b65-a204-29359d4f8f7c.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(21, 17, 'Crumbed Chicken Wrap', NULL, 45.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792491525/edueats/menu-items/b9679554-5cb4-45a1-8367-af01be2140b6.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(22, 17, 'Cheese Sandwich', NULL, 25.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792491654/edueats/menu-items/0e8a19e8-7591-4278-a019-13d7bea21471.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(23, 17, 'Chicken Mayo Sandwich', NULL, 40.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792491883/edueats/menu-items/18b0defe-ff3a-43ee-80f9-830717feedf2.jpg', 4, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(24, 18, 'Chip Roll', NULL, 25.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792492726/edueats/menu-items/bfa8af55-7145-45ce-a7e4-b9e57f0a7911.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(25, 18, 'Russian Roll', 'Add chips for R10.', 30.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792492802/edueats/menu-items/33e9e0ec-b9e3-4a2c-8718-7ae3761a4f61.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(26, 18, 'Chicken Strips (5 pieces)', 'Add chips for R10.', 40.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792492950/edueats/menu-items/87e14c7b-cde2-4727-8812-b4f7d21d96d1.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(27, 18, 'Hot Chips', 'Small or large.', 20.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792493156/edueats/menu-items/5f5281f2-4e78-4ef2-aaec-08b271ddc70c.jpg', 4, '2026-10-21 13:48:15', '2026-10-21 14:33:00', 30),
(28, 18, 'Greek Salad', NULL, 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792493380/edueats/menu-items/2f3803b3-81f6-4e88-98d3-803b5f5bd677.jpg', 5, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(29, 19, 'Butter Chicken', NULL, 60.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792493550/edueats/menu-items/ea566a9f-358d-415a-9584-3b42a8f72e61.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(30, 19, 'Pizza Slice', NULL, 30.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792493780/edueats/menu-items/bdcb0b6c-79c9-4e03-a65d-57ec676d6ba0.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(31, 19, 'Macaroni & Cheese', NULL, 55.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792493947/edueats/menu-items/67e2d008-abe7-4e5c-96a1-7eac88893f07.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(32, 20, 'Coffee', NULL, 15.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792494048/edueats/menu-items/abc20ee9-3c5f-4bab-8510-c84983f87ded.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(33, 20, 'Tea', 'Ceylon or rooibos tea.', 15.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792494312/edueats/menu-items/ad889c4e-4644-4069-8abe-290294ecf83a.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(34, 20, 'Hot Chocolate', NULL, 30.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792494331/edueats/menu-items/c830af3d-caf8-4820-9856-f7dbad596f9d.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(35, 21, 'Jive 500ml', NULL, 13.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497583/edueats/menu-items/914e2b97-7391-454a-8c16-e8cdcea21172.png', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(36, 21, 'Juice', NULL, 18.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497610/edueats/menu-items/83ea39ed-6683-4cd6-86af-94f8e6e3360c.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(37, 21, 'Water 500ml', NULL, 10.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497664/edueats/menu-items/8e20f646-5d9e-4430-aedd-7e0d9a32121b.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(38, 21, 'Red Bull', NULL, 20.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497680/edueats/menu-items/18a19169-71a7-4ce2-8af8-ce7d127b579a.jpg', 4, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(39, 21, 'Passion Fruit Mocktail', NULL, 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497731/edueats/menu-items/75395941-d919-4b0b-95ef-8ed108bded16.jpg', 5, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(40, 21, 'Watermelon Mint Mocktail', NULL, 35.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497773/edueats/menu-items/ef117e10-93c3-4b54-9f0c-8f53c8ddda2e.jpg', 6, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(41, 3, 'Brownie', NULL, 16.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497786/edueats/menu-items/e84b5416-82bd-4497-99cd-8f361f71200a.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(42, 3, 'Cheesecake', NULL, 17.50, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497833/edueats/menu-items/115ad142-ed9a-414e-9589-396624a4ad86.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(43, 3, 'Cinnamon Donut', NULL, 10.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497947/edueats/menu-items/295fff95-d22e-40a5-8dc6-1546aa6282e4.jpg', 3, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(44, 23, 'Lay\'s 36g', NULL, 12.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792497972/edueats/menu-items/2cb8a1f2-bed4-4c23-8d31-3ae4880f79c3.jpg', 1, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30),
(45, 23, 'Doritos', NULL, 12.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792498017/edueats/menu-items/67930a0a-b14a-4a0d-b8dc-8d271ba75a90.jpg', 2, '2026-10-21 13:48:15', '2026-10-21 14:33:01', 30);

-- --------------------------------------------------------

--
-- Table structure for table `menu_item_modifier_groups`
--

CREATE TABLE `menu_item_modifier_groups` (
  `menu_item_id` bigint(20) NOT NULL,
  `modifier_group_id` bigint(20) NOT NULL,
  `display_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `menu_item_modifier_groups`
--

INSERT INTO `menu_item_modifier_groups` (`menu_item_id`, `modifier_group_id`, `display_order`, `created_at`, `updated_at`) VALUES
(10, 8, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(10, 13, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(11, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(11, 9, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(12, 10, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(13, 14, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(14, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(14, 9, 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(14, 10, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(15, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(15, 9, 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(15, 10, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(16, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(16, 9, 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(16, 10, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(17, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(17, 9, 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(17, 10, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(18, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(18, 9, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(19, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(19, 9, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(20, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(20, 9, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(21, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(21, 9, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(22, 15, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(23, 15, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(24, 8, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(25, 8, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(25, 9, 3, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(25, 11, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(26, 8, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(26, 11, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(27, 8, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(27, 12, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(32, 16, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(33, 17, 2, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(33, 18, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(34, 17, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15');

-- --------------------------------------------------------

--
-- Table structure for table `modifiers`
--

CREATE TABLE `modifiers` (
  `modifier_id` bigint(20) NOT NULL,
  `modifier_group_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `added_price` decimal(10,2) DEFAULT 0.00,
  `available` tinyint(1) DEFAULT 1,
  `display_order` int(11) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `modifiers`
--

INSERT INTO `modifiers` (`modifier_id`, `modifier_group_id`, `name`, `added_price`, `available`, `display_order`, `created_at`, `updated_at`) VALUES
(9, 8, 'Tomato sauce', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(10, 8, 'BBQ sauce', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(11, 8, 'Garlic mayo', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(12, 8, 'Mayo', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(13, 8, 'Chilli sauce', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(14, 9, 'No onions', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(15, 9, 'No tomato', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(16, 9, 'No lettuce', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(17, 9, 'No sauce', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(18, 10, 'Fried onions', 5.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(19, 10, 'Avocado', 15.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(20, 11, 'With chips', 10.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(21, 12, 'Small', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(22, 12, 'Large', 20.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(23, 13, 'Scrambled', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(24, 13, 'Sunny-side up', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(25, 14, '1 topping and syrup', 15.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(26, 15, 'Toasted', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(27, 16, 'Extra shot', 6.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(28, 16, 'Syrup', 10.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(29, 16, 'Oat milk', 10.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(30, 17, 'Oat milk', 10.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(31, 18, 'Ceylon', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15'),
(32, 18, 'Rooibos', 0.00, 1, 0, '2026-10-21 13:48:15', '2026-10-21 13:48:15');

-- --------------------------------------------------------

--
-- Table structure for table `modifier_groups`
--

CREATE TABLE `modifier_groups` (
  `modifier_group_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `minimum_selections` int(11) DEFAULT 0,
  `maximum_selections` int(11) DEFAULT 1,
  `available` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `is_required` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `modifier_groups`
--

INSERT INTO `modifier_groups` (`modifier_group_id`, `name`, `minimum_selections`, `maximum_selections`, `available`, `created_at`, `updated_at`, `is_required`) VALUES
(8, 'Sauce', 0, 2, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(9, 'Leave out', 0, 4, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(10, 'Extras', 0, 2, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(11, 'Add chips', 0, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(12, 'Size', 1, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'1'),
(13, 'Egg style', 1, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'1'),
(14, 'Toppings', 0, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(15, 'How would you like it?', 0, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(16, 'Coffee extras', 0, 3, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(17, 'Milk', 0, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'0'),
(18, 'Tea', 1, 1, 1, '2026-10-21 13:48:15', '2026-10-21 13:48:15', b'1');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `notification_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `type` varchar(50) NOT NULL,
  `title` varchar(50) NOT NULL,
  `message` text NOT NULL,
  `channel` varchar(50) NOT NULL,
  `delivery_status` varchar(50) NOT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `read_at` timestamp NULL DEFAULT NULL,
  `sent_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `order_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `status_id` bigint(20) NOT NULL,
  `customer_order_number` varchar(20) DEFAULT NULL,
  `collected_at` timestamp NULL DEFAULT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

CREATE TABLE `order_items` (
  `order_item_id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `menu_item_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL,
  `menu_item_name_at_purchase` varchar(150) NOT NULL,
  `price_at_purchase` decimal(10,2) NOT NULL,
  `special_instructions` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_item_modifiers`
--

CREATE TABLE `order_item_modifiers` (
  `order_item_modifier_id` bigint(20) NOT NULL,
  `order_item_id` bigint(20) NOT NULL,
  `modifier_id` bigint(20) NOT NULL,
  `modifier_name_at_purchase` varchar(150) NOT NULL,
  `price_at_purchase` decimal(10,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_status`
--

CREATE TABLE `order_status` (
  `status_id` bigint(20) NOT NULL,
  `status_name` varchar(50) NOT NULL,
  `sort_order` int(11) DEFAULT 0,
  `is_final` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `order_status`
--

INSERT INTO `order_status` (`status_id`, `status_name`, `sort_order`, `is_final`, `created_at`, `updated_at`) VALUES
(1, 'Submitted', 1, 0, '2026-07-10 12:14:01', '2026-07-10 12:14:01'),
(2, 'Accepted', 2, 0, '2026-07-10 12:14:01', '2026-07-10 12:14:01'),
(3, 'Preparing', 3, 0, '2026-07-10 12:14:01', '2026-07-10 12:14:01'),
(4, 'Ready', 4, 0, '2026-07-10 12:14:01', '2026-07-10 12:14:01'),
(5, 'Collected', 5, 1, '2026-07-10 12:14:01', '2026-07-10 12:14:01'),
(6, 'Cancelled', 6, 1, '2026-07-10 12:14:01', '2026-07-10 12:14:01');

-- --------------------------------------------------------

--
-- Table structure for table `order_status_history`
--

CREATE TABLE `order_status_history` (
  `status_history_id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `status_id` bigint(20) NOT NULL,
  `changed_by_user_id` bigint(20) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `refresh_tokens`
--

CREATE TABLE `refresh_tokens` (
  `id` bigint(20) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `refresh_tokens`
--

INSERT INTO `refresh_tokens` (`id`, `token_hash`, `user_id`, `expires_at`, `revoked`, `created_at`) VALUES
(93, '39c9905d5c1f7dbf15641e79aed8bfca39120307584b203e152b202004d2582c', 10, '2026-10-18 11:26:47.000000', 1, '2026-10-11 11:26:47.000000'),
(94, '3a1555001e815dd1e9343e4cf2545d0ccf148d458418085423cbcd3cc0fe6252', 10, '2026-10-18 11:28:49.000000', 0, '2026-10-11 11:28:49.000000'),
(95, '092743f63d3a9b32ebce102f336cd2dd9ea7e7166b2d7eff742cb9740695dbec', 10, '2026-10-19 08:14:35.000000', 0, '2026-10-12 08:14:35.000000'),
(96, 'dc937672b98ac443ac2989ad7522c3479dc4d31d4e9ce458959062a9d9c48cb2', 10, '2026-10-19 09:57:44.000000', 0, '2026-10-12 09:57:44.000000'),
(97, '8adf278a980dc80f6ee7f2184553514f6b96ea1ebf84e9dd409d9b6af22fd078', 10, '2026-10-19 10:06:35.000000', 1, '2026-10-12 10:06:35.000000'),
(98, '8898a55498902cb9efbb8ebc950ffe5458996e014d14e7ee4b78212b4d9b1ef9', 10, '2026-10-19 10:08:20.000000', 1, '2026-10-12 10:08:20.000000'),
(99, '2ad040a76c38c52eebefe2e1883615f17896ef0406f52f386f07e7fa34a5932e', 10, '2026-10-19 10:30:36.000000', 1, '2026-10-12 10:30:36.000000'),
(100, 'dc7211f4a272bc368a346e6801d9cc79b503b494ddc8e88168f5622566c0ad81', 10, '2026-10-19 12:15:12.000000', 1, '2026-10-12 12:15:12.000000'),
(101, '43d313733fc8587dd93c3617de7518fd071452d1a0157ff156c407d700417b8e', 10, '2026-10-19 12:15:22.000000', 1, '2026-10-12 12:15:22.000000'),
(102, 'fb521bf7d1eb333452a5d065f54cdd8558b557cd3d9f702b5634d9c453c4c08b', 10, '2026-10-19 12:19:32.000000', 0, '2026-10-12 12:19:32.000000'),
(103, '762d7162b693a0bc82ef2158b01c7f791c077480a9bb8cf37d387b5b644789a6', 10, '2026-10-19 12:21:08.000000', 1, '2026-10-12 12:21:08.000000'),
(104, '573646fb0baa4d794b2514c708aed8c4838a8fd84cf9c80b19143eea7ea46934', 10, '2026-10-19 12:35:26.000000', 1, '2026-10-12 12:35:26.000000'),
(105, 'a4febe9c38f9f485aa3b1c4ec18aba25f7ab4396203b9fcb0de3b837762ec60e', 10, '2026-10-19 12:36:30.000000', 1, '2026-10-12 12:36:30.000000'),
(106, '3ed70e4cc6fb9deaf00e9eff352ec027666b4d749926902b080a824040018baa', 10, '2026-10-20 09:29:35.000000', 1, '2026-10-13 09:29:35.000000'),
(107, '2cda8738ffea14550aa342ddc7523d3b36a0e826b63c4c9ff1734c64d347ca51', 10, '2026-10-20 09:30:11.000000', 1, '2026-10-13 09:30:11.000000'),
(108, 'bc21b716171c22502f98beb0fab3cb2c90ea0b9ee998bb4e4d1fe9e90699b8c8', 10, '2026-10-20 09:43:49.000000', 1, '2026-10-13 09:43:49.000000'),
(109, 'c412c663e0cb67f3d2d869f81c989b354b6cf9c0b6ea59c844eb9674bf9fb3a7', 10, '2026-10-20 09:44:01.000000', 1, '2026-10-13 09:44:01.000000'),
(110, 'a696062406d3dd506086d61894a8b65243b64849dec8c39fa6b956aa848e29f3', 10, '2026-10-20 11:04:16.000000', 1, '2026-10-13 11:04:16.000000'),
(111, 'e9e0785e99f2a3d806a765645f50f61ad5287cf9267101a980a5e0bf21101c27', 10, '2026-10-20 11:07:04.000000', 1, '2026-10-13 11:07:04.000000'),
(112, 'f4faef6ec2633ce6214d0cd67bc6ee7c1a6ab49e121e634efacbceaa5e3ba79d', 10, '2026-10-20 11:31:19.000000', 1, '2026-10-13 11:31:19.000000'),
(113, '6317daa2262f558ec34b268d1330d0d64abb8f941582366bab13e563f82cad1d', 10, '2026-10-20 11:49:42.000000', 0, '2026-10-13 11:49:42.000000'),
(114, 'fe295d49707a8aaf7bd046722bcd797c806befd3fad4a388350ec4a5a4d12ae8', 10, '2026-10-20 13:23:05.000000', 1, '2026-10-13 13:23:05.000000'),
(115, '07aea16de4becf02d3c98f569f84fb96c5c2aace609046798222a9c95945abdc', 10, '2026-10-20 13:23:30.000000', 1, '2026-10-13 13:23:30.000000'),
(116, 'bd6229b2e53c34f39f5f67dcdb95c5e251a0a62d51830642205a9b050094acc9', 10, '2026-10-20 13:31:26.000000', 1, '2026-10-13 13:31:26.000000'),
(117, 'a794b3073c42867d95873903eeca231ea7023c5ed86fb12811f31a4baa7e465f', 10, '2026-10-20 14:38:38.000000', 1, '2026-10-13 14:38:38.000000'),
(118, '6bea29081a9804828ea5a3991309bf162f97f38c2adb7b9f4d3a3a46f2733123', 10, '2026-10-21 08:53:05.000000', 1, '2026-10-14 08:53:05.000000'),
(119, 'a02bb9eda515c5c100367d813c000f28d51ad18f1026fa01fcfc30fd17185c26', 10, '2026-10-21 08:53:31.000000', 1, '2026-10-14 08:53:31.000000'),
(120, 'de136d904fd65d727a209a784a7c739f0156ddbfb27d0f4168ccd35758613a46', 17, '2026-10-21 09:08:27.000000', 0, '2026-10-14 09:08:27.000000'),
(121, '9bc1c085a3e10361f48973a271134f4479a5dd34c6b48291083d0e2d0a6708d5', 17, '2026-10-21 09:09:12.000000', 1, '2026-10-14 09:09:12.000000'),
(122, '1629d52abad29693e073d094930710c6bb0475ad97db531a00f5d0533913d6d2', 17, '2026-10-21 09:10:21.000000', 1, '2026-10-14 09:10:21.000000'),
(123, '356b564310164937167d36c572fcf83ad4fcaec51bd86bdac9af84822a208d55', 18, '2026-10-21 09:11:57.000000', 0, '2026-10-14 09:11:57.000000'),
(124, 'f2a826abb728ece4f3efc9e6d3245029d36ffc0e7d7a24ce7e8ef5c82d46e7cc', 18, '2026-10-21 09:12:05.000000', 0, '2026-10-14 09:12:05.000000'),
(125, '3e2d67c6b02cf41b84f6e2447f0a1aa78a6dd6dc168e1931f90e1c5d02310865', 17, '2026-10-21 09:13:47.000000', 1, '2026-10-14 09:13:47.000000'),
(126, 'b1eafd324a938abb8295d9c1943ca1c210477ae450a48760b54faa0683a16bff', 17, '2026-10-21 09:14:11.000000', 1, '2026-10-14 09:14:11.000000'),
(127, 'b559a26d0d85249b01226791323bad41e86c04d8999ababd3b8da323f36172c6', 17, '2026-10-21 09:14:47.000000', 1, '2026-10-14 09:14:47.000000'),
(128, 'e48786a08d53d31457a3b67ac21a7c7e82b0ee71f3a50d215b23167ac230dd38', 10, '2026-10-21 09:16:07.000000', 1, '2026-10-14 09:16:07.000000'),
(129, 'ecdd7f0879dcd69ba8535d1061b81236d18c5d7062ba2228897c14d25caed29a', 17, '2026-10-21 09:31:09.000000', 1, '2026-10-14 09:31:09.000000'),
(130, '1337c1f5e4289aa23b4fcf2f38b44e97e11022b973c00d6c2c95aa75de798bac', 17, '2026-10-21 09:31:46.000000', 1, '2026-10-14 09:31:46.000000'),
(131, '6ffd583bb08e65e1293744b4c8f7c1bb4af5b1d058b1f115e86805a9a2d07fd2', 17, '2026-10-21 09:32:16.000000', 1, '2026-10-14 09:32:16.000000'),
(132, '01f50e6409c64f170ec089e62322229c97ebe5cd7bcedf19a2dd34d9cb5c1879', 10, '2026-10-21 14:32:27.000000', 0, '2026-10-14 14:32:27.000000'),
(133, 'f1de394f99c76ea1610ab07b9db554c787b345b4d8968014385a1e91ffe6e14b', 10, '2026-10-21 14:35:40.000000', 1, '2026-10-14 14:35:40.000000'),
(134, 'a899a81bca7549ffbf836968c6c6ad5ba679fa1a6cfce69475ddb9bfa08651c0', 10, '2026-10-21 14:38:34.000000', 0, '2026-10-14 14:38:34.000000'),
(135, '3ec870e0cad60349cd77640432f4ca4fe284751c8eb1b2ecd33be626526c2859', 10, '2026-10-22 07:07:12.000000', 1, '2026-10-15 07:07:12.000000'),
(136, '1f6d93794eeb21b5441632a072447485745f33afacbb2831c24cd812898e6086', 10, '2026-10-22 07:17:06.000000', 1, '2026-10-15 07:17:06.000000'),
(137, '20406760a0484bfd354f071e01083c552d8f62f9f7ba727a21c2f9a966cef7f6', 10, '2026-10-22 07:26:10.000000', 0, '2026-10-15 07:26:10.000000'),
(138, 'eaccd4f22815da42948befe2bae3cf081bd9cc4e0dd1305b50cd31db5ac2f4ae', 10, '2026-10-22 07:27:44.000000', 1, '2026-10-15 07:27:44.000000'),
(139, 'bc08eac3a775e42718f0ec5d187dd44b5a0b298f59d33c2cfe5a585d239f7a6d', 10, '2026-10-22 07:41:10.000000', 1, '2026-10-15 07:41:10.000000'),
(140, 'd2fef7dd7be21967f6cff150a04349da5bcebbad26df702fb04c977e7e72997b', 10, '2026-10-22 07:42:44.000000', 1, '2026-10-15 07:42:44.000000'),
(141, '1c700eeaa4b2eb5ed9b7a8dc6ece501ec8284661bf54284a9c65599ad2e8ad6c', 10, '2026-10-22 07:57:44.000000', 1, '2026-10-15 07:57:44.000000'),
(142, '6527ff8361e83837ad7d3170e696ba8eef755bcc9f123dc50850aa8199b78cf5', 10, '2026-10-22 08:12:44.000000', 1, '2026-10-15 08:12:44.000000'),
(143, 'ad8879ff714f1d32e6a5377599940d6f5c9ca1b78926a594ca9cd87be8d54b8d', 10, '2026-10-22 09:33:02.000000', 1, '2026-10-15 09:33:02.000000'),
(144, '2fbe411d3dd419bf1405e92374ab099ad32763db50cc51baa70954bae84e1015', 10, '2026-10-22 09:33:11.000000', 1, '2026-10-15 09:33:11.000000'),
(145, '167ed8d864376ee857c5378dd3d2e4ff617e70660763798e12fdca57a4e28bd5', 10, '2026-10-22 09:33:35.000000', 1, '2026-10-15 09:33:35.000000'),
(146, 'dd0a2688fa3d7a7497121225c50cf9fafca0205b9386029f527b12590899500f', 10, '2026-10-22 09:40:58.000000', 1, '2026-10-15 09:40:58.000000'),
(147, 'a9d3a187178899082767566a1963deaa13fd6f6030a05904fb0c055e4b308cb0', 10, '2026-10-22 09:41:26.000000', 1, '2026-10-15 09:41:26.000000'),
(148, '0e1d84b369a00a3266880c423063354740dc3897aed8c8a2059ca2b455922fdc', 10, '2026-10-22 10:59:02.000000', 1, '2026-10-15 10:59:02.000000'),
(149, '7ef7153c1719cdfdad03bfaa4a76da563c04073b0b0430e11f0fcee1747c3cc6', 10, '2026-10-22 10:59:36.000000', 1, '2026-10-15 10:59:36.000000'),
(150, 'eb6985ffd96424f60632575b32e899ee402a1538ac4387163038284b5fdef13e', 10, '2026-10-22 11:05:28.000000', 1, '2026-10-15 11:05:28.000000'),
(151, 'f9bc0e85b50b966efa16ea3a948a24e933bc7de6003e9009e804384fe0578aaf', 10, '2026-10-22 11:05:52.000000', 1, '2026-10-15 11:05:52.000000'),
(152, '3ad2f1d77eff84eac98763c75dfdd423ae83c00ff27aad060e0008d640f21588', 10, '2026-10-22 12:27:13.000000', 0, '2026-10-15 12:27:13.000000'),
(153, '3b09121ed070a8b6d6780686ad6ba55c58ac8b9efe77f1be208a423f47096a1a', 10, '2026-10-23 07:50:33.000000', 1, '2026-10-16 07:50:33.000000'),
(154, 'c98d3577e45b2e5f03e905987e097d31fd6a0b1f68398663471ef816b9e1198f', 10, '2026-10-23 09:05:31.000000', 1, '2026-10-16 09:05:31.000000'),
(155, 'f928bc6c22843f2fa4d218ec3c113d11ef1de3f60c61d3a791ed6733e2231a1d', 10, '2026-10-23 09:05:41.000000', 1, '2026-10-16 09:05:41.000000'),
(156, '72b624adbabe1dfe5db204653ae1f61735766ddea136a4c94ab6ff21c8be79c5', 10, '2026-10-23 09:10:25.000000', 1, '2026-10-16 09:10:25.000000'),
(157, '1a40abf00da3a46ff71c12c3dbac16bf6a739ea10ea85c822650f2c6b6ed255e', 10, '2026-10-23 09:10:52.000000', 1, '2026-10-16 09:10:52.000000'),
(158, '04df3f9c263b7a03a91218f44b677e3b99ebf2635df30019ea64a5b81270d01d', 10, '2026-10-23 09:50:16.000000', 1, '2026-10-16 09:50:16.000000'),
(159, '060b87fd5b8be5258375d9489250623e56b71313aac34f1b2c94ba982d7862ea', 10, '2026-10-23 10:07:12.000000', 1, '2026-10-16 10:07:12.000000'),
(160, '58ccd2c7241da3c05faeaa26c86cf3ddd2b85b25a865dc0a0f95649225e0d650', 18, '2026-10-23 12:55:38.000000', 1, '2026-10-16 12:55:38.000000'),
(161, '00c4ce15214560fb56c21b1e29f5a2f9ff23c70ccf4167259ac851c52dc3f96a', 10, '2026-10-23 12:56:54.000000', 0, '2026-10-16 12:56:54.000000'),
(162, '301925228e920d5324d6a7a92ea523c58ea6f8f806dd091ac15a6d4b7f35737a', 18, '2026-10-23 12:57:26.000000', 1, '2026-10-16 12:57:26.000000'),
(163, '0b72d80f24547801e3edb7ef7a62059c80a1c841d9e1142b4ea493e1ec6f4084', 10, '2026-10-23 12:57:41.000000', 0, '2026-10-16 12:57:41.000000'),
(164, '7c8eb540703b7fbe7451aa4184da4f6fe38b5144bd6e375e5e1a0d9bcc03b502', 10, '2026-10-23 12:58:02.000000', 0, '2026-10-16 12:58:02.000000'),
(165, 'fc5ce49b4be36ac2dfa02c545fbd05c517ff46d66c631e6ec96894ecab14976a', 18, '2026-10-23 12:58:32.000000', 0, '2026-10-16 12:58:32.000000'),
(166, 'a45c85c9fa120b2ad402741462566f9ab6c9df75945532286a815b41f7125ee9', 10, '2026-10-23 13:10:10.000000', 0, '2026-10-16 13:10:10.000000'),
(167, '3ff579c06cff55f1be265e8a819a9a939437888cb3a10326080ebbd3f3459a5b', 15, '2026-10-23 13:11:16.000000', 1, '2026-10-16 13:11:16.000000'),
(168, 'c4efacee2aa4edf2bfd1fe92d65d57fe204eb506c455d424abac3ac4217b7fd3', 15, '2026-10-23 13:12:08.000000', 0, '2026-10-16 13:12:09.000000'),
(169, 'ff7117e95c2b4ce1abe2b065e973c3681eedf7e1b2758fa0d09f3d89b607eb39', 10, '2026-10-23 13:12:15.000000', 0, '2026-10-16 13:12:15.000000'),
(170, 'cb04cc8d017302a8775488b82d509305194c077734b395b4c1f93e52b175e076', 10, '2026-10-23 13:12:24.000000', 0, '2026-10-16 13:12:24.000000'),
(171, '28f3859286606fc33d8ecee0ef3a8b1fec085b2b1d8f3aa8bea50e37ca4ad043', 10, '2026-10-23 13:12:35.000000', 0, '2026-10-16 13:12:35.000000'),
(172, '21097f86f0f36e79c7df2f81c84a7c3713bdea4465d074a63fd88f94889e9055', 10, '2026-10-23 13:14:15.000000', 0, '2026-10-16 13:14:15.000000'),
(173, '747518f34cf150ce54757ab5d87385a08ee74621df80af392407eee26a78b8e7', 10, '2026-10-23 13:14:22.000000', 0, '2026-10-16 13:14:22.000000'),
(174, 'f13e7c6c7a4cf264b36c0ea1366586c8c6642ba2e7376b0b79a79fcaf723a155', 10, '2026-10-23 14:08:30.000000', 1, '2026-10-16 14:08:30.000000'),
(175, '92a68da8503dfe02659aa2675cea8f03b7ce3575d62fed8ff6dce56bf7bb0ad4', 10, '2026-10-23 14:10:44.000000', 0, '2026-10-16 14:10:44.000000'),
(176, '9fff0463ff63a2f96d873657326dad2f524e84991001416345f0de9e8f61bcb3', 15, '2026-10-23 14:10:47.000000', 1, '2026-10-16 14:10:47.000000'),
(177, '104cf0319f4f3598e15982dd7b393f853ac2c849ecc64aa75fb29e718e4671f3', 19, '2026-10-23 14:18:20.000000', 0, '2026-10-16 14:18:20.000000'),
(178, 'a071149ac9c9194829fbeef5dd47053ba094802b8d32d8283e716d0e354c3196', 19, '2026-10-23 14:18:43.000000', 0, '2026-10-16 14:18:43.000000'),
(179, '237389b9bd8aee237ae32061f1913ec6a8bd3fc566823a544a1679d501a4fa33', 10, '2026-10-24 09:35:54.000000', 0, '2026-10-17 09:35:54.000000'),
(180, '6a34a0690cd994b2a9c98d2833d420254a173021afd1c4c02a7365aba2aaa9ae', 10, '2026-10-24 09:36:50.000000', 0, '2026-10-17 09:36:50.000000'),
(181, 'bcff501cb7f15ae78e867286b524b86b2652638b25a06fef1212d0ed4cfed958', 10, '2026-10-24 09:38:57.000000', 0, '2026-10-17 09:38:57.000000'),
(182, 'ec4cdbf887f1876f1e02da22befecf671d626a5bef9d7cae50d72f475d937884', 10, '2026-10-24 09:40:53.000000', 0, '2026-10-17 09:40:53.000000'),
(183, '8d24cb5e60fe994606ff08be3c49d7ccdb012024959913ab13f83218509a469b', 10, '2026-10-24 11:41:15.000000', 0, '2026-10-17 11:41:15.000000'),
(184, 'a49b92a09a592d63ca6650c8901c5756ff5a0a8bc0d599b7cd7dbf105a99f141', 10, '2026-10-24 11:53:50.000000', 0, '2026-10-17 11:53:50.000000'),
(185, 'cc2abf7822e37babe6aab31411df64ec5512c6d078f4f6179e3c45d55e1c7e5e', 15, '2026-10-24 11:55:01.000000', 1, '2026-10-17 11:55:01.000000'),
(186, '5dbef378f03b8df45059e419d07069119199683940d745da4c2f2b2886b47bff', 10, '2026-10-24 12:23:18.000000', 0, '2026-10-17 12:23:19.000000'),
(187, '6482d8d2feff860d42c457cd7335c341cfae7428ce02a66dcc2f55dac4df3a23', 10, '2026-10-24 19:15:16.000000', 0, '2026-10-17 19:15:16.000000'),
(188, '5d9b51a23f32df666cca17ebde240c0788c8e1245d14983bfe5df7c7a391b535', 10, '2026-10-25 08:00:56.000000', 0, '2026-10-18 08:00:56.000000'),
(189, '3238cd63fb312b02912836641b10d618f12f125f2902c8a5321cee7091cea2c8', 10, '2026-10-25 14:55:58.000000', 0, '2026-10-18 14:55:58.000000'),
(190, '33a306dcc1b3bb44c730729d4492736de5354f63b8ca0a9b1f11677dc362d4e0', 10, '2026-10-25 14:57:01.000000', 0, '2026-10-18 14:57:01.000000'),
(191, '1e2e3ed758783e7039729b607ce832041975af5bb941ac5579463704830d4468', 10, '2026-10-26 09:12:50.000000', 0, '2026-10-19 09:12:50.000000'),
(192, '29973322f163daeb302e012453f2b9fe6c30b4c242237bc4bfc6010b4e71169b', 10, '2026-10-26 09:15:01.000000', 0, '2026-10-19 09:15:01.000000'),
(193, '6c469c3607d747ec72ae6fe9f2191a837e586313ef522b2d0cd15ad339e2813d', 10, '2026-10-26 10:32:53.000000', 0, '2026-10-19 10:32:53.000000'),
(194, 'bd5cf8121b3ee24d1215f0f3d828f3cffb646524dc377fc92929929019b1d9af', 10, '2026-10-26 10:43:19.000000', 1, '2026-10-19 10:43:19.000000'),
(195, '134c8b418cca40788c2f6c99d296193c88f7b74328eca274a5761f659f867d32', 10, '2026-10-26 11:31:21.000000', 1, '2026-10-19 11:31:21.000000'),
(196, '9f046552e2fddc2789825ffeb8bb06484609b7b0d914be62de84ff4f66f4ac3f', 10, '2026-10-26 12:09:16.000000', 1, '2026-10-19 12:09:17.000000'),
(197, '76f4c43845c7398272799aa567006a78dcb9c073017afc36ad2598dccaa73917', 10, '2026-10-26 12:24:20.000000', 1, '2026-10-19 12:24:20.000000'),
(198, 'c48ca88f3dc2e30ad6f4299c656a8c28b3fcf15938719524d9739ce32922a7e7', 10, '2026-10-26 12:39:20.000000', 1, '2026-10-19 12:39:20.000000'),
(199, 'd8b8b299ef8d81a9c6a7754b6bade7c15ae81191b76dba0f4c9b41c74948fba9', 10, '2026-10-26 12:54:20.000000', 1, '2026-10-19 12:54:20.000000'),
(200, 'eb86a86f6a69d96dacff45a61c96ba31b77584668613949c4834fb697ecf9f00', 10, '2026-10-26 13:09:20.000000', 0, '2026-10-19 13:09:20.000000'),
(201, 'a44f7769afc46e37e6d412072042ceb45875977128120dfd1c42c8b8eab15537', 10, '2026-10-27 13:41:07.000000', 1, '2026-10-20 13:41:07.000000'),
(202, 'c8cd5320d876fed0b009f01ca6060bc744bda33bb48f45a2225f3f8fe7318f67', 10, '2026-10-27 14:22:28.000000', 0, '2026-10-20 14:22:28.000000'),
(203, 'dfacf861a655181512f49c8dd41550db2c733e7fadecb68ad5a65d5e19f3eb24', 10, '2026-10-28 13:17:54.000000', 1, '2026-10-21 13:17:54.000000'),
(204, 'ad9f442ad770aab2f00b40236a189aecc748145431717086e59323df341bbd3a', 10, '2026-10-28 13:33:31.000000', 0, '2026-10-21 13:33:31.000000'),
(205, 'f1befa381df5ac6236fd06424d9625be192e96720c813fc5ee504fe7e2aa1250', 15, '2026-10-28 13:51:10.000000', 1, '2026-10-21 13:51:10.000000');

-- --------------------------------------------------------

--
-- Table structure for table `roles`
--

CREATE TABLE `roles` (
  `role_id` bigint(20) NOT NULL,
  `role_name` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `roles`
--

INSERT INTO `roles` (`role_id`, `role_name`, `created_at`, `updated_at`) VALUES
(1, 'ADMIN', '2026-07-10 12:14:01', '2026-09-28 10:33:24'),
(2, 'STAFF', '2026-07-10 12:14:01', '2026-09-28 10:33:24'),
(3, 'STUDENT', '2026-07-10 12:14:01', '2026-09-28 10:33:24'),
(4, 'CUSTOMER', '2026-09-18 09:44:34', '2026-09-28 10:33:24');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` bigint(20) NOT NULL,
  `customer_code` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  `active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `last_login` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `customer_code`, `first_name`, `last_name`, `email`, `phone_number`, `password_hash`, `role_id`, `active`, `created_at`, `updated_at`, `last_login`) VALUES
(1, 'STU99887', 'Alice', 'Smith', 'alice@campus.edu', NULL, '$2a$10$7R.v/u3xZ.bV9N9IeU.kZgQfE8Jg1u3u0i7cZ/2wD3q1234567890', 2, 1, '2026-07-10 14:05:36', '2026-10-16 09:14:10', NULL),
(4, 'CUST-F1BCB0', 'Bob', 'Jones', 'bob@campus.edu', NULL, '$2a$10$h9Kt.sI061ynenuRduInq.eEsq9GsuyQrgrM5O34QEhq3rx2HU10u', 4, 1, '2026-09-18 09:53:50', '2026-10-17 19:17:14', NULL),
(5, 'CUST-585FB1', 'Charlie', 'Brown', 'charlie@campus.edu', NULL, '$2a$10$giHY3ul6ViF23Afsg0.7U.ErWT2v3RMS31kBPazBl5VP9B2l7VN5C', 1, 1, '2026-09-20 10:44:29', '2026-09-20 10:53:22', NULL),
(6, 'CUST-32D494', 'Charlie', 'Brown', 'charlie2@campus.edu', NULL, '$2a$10$0rQy1jNLRXEKZ3fVyJLoSuCROUTl0zrWrz9taZeA4DogKIYOzJtqq', 1, 1, '2026-09-26 10:56:00', '2026-09-28 07:36:25', NULL),
(7, 'STU12345', 'John', 'Doe', 'john.doe@campus.edu', NULL, '$2a$10$e7...', 3, 1, '2026-09-28 07:32:39', '2026-09-28 07:32:39', NULL),
(8, 'CUST-6FA8C8', 'Charlie', 'Brown', 'charli22222e@campus.edu', NULL, '$2a$10$B3ac2/EPpyXoTqrWrFItY.TZOrXwXI9y30HjfVearAhzLv0SVqnKa', 4, 1, '2026-09-29 13:05:33', '2026-09-29 13:05:33', NULL),
(9, 'CUST-E3CF40', 'Refresh', 'Tester', 'refresh@test.com', NULL, '$2a$10$CrGKzNwXLM6nU7D9qqsEJeSz1AE/vanQrdM8MWYEDNHpHrlrjXGUa', 4, 1, '2026-10-01 13:39:52', '2026-10-01 13:39:52', NULL),
(10, 'CUST-C8FCCF', 'paul', 'johnson', 'john@campus.edu', NULL, '$2a$10$H.QLaDYbnUyDD.nqVzRJaOab8FancP3btWSsT/NpxuCw1K5bhC7JS', 1, 1, '2026-10-02 08:57:36', '2026-10-21 13:17:54', '2026-10-21 15:17:54'),
(11, 'CUST-300279', 'Staff', 'Member', 'staff.membe9r@eduvos.com', NULL, '$2a$10$IqkKkQoMKlAdcQRdiKT6jesbhSmQ7kRYlLWHbgR8dEYRb4AsJNyBC', 3, 1, '2026-10-02 09:02:40', '2026-10-02 09:02:40', NULL),
(12, 'CUST-7342D0', 'Staff3', 'Member3', 'staff.membe9dsadasr@eduvos.com', NULL, '$2a$10$iDA9rYw0jIEjbXa6JQj4t.lPK76YqjjmOGyqoSZ2xiG1FXYWbLSzm', 3, 1, '2026-10-03 10:56:44', '2026-10-03 10:56:44', NULL),
(13, 'CUST-72EBBF', 'Dixie', 'Normous', 'dnormous@big.com', NULL, '$2a$10$NRmVXeqQwAy69c0jA27MUePJsRecoSsG2Ak2rs.r5pLjlpeDqrOnq', 4, 1, '2026-10-06 12:52:57', '2026-10-06 12:52:57', NULL),
(14, 'CUST-C597E6', 'Abc', 'Def', 'a@b.com', NULL, '$2a$10$oQvuRmcd70v5Vmxb1oMNq.o780987dfv77T07zzIrQNYF.0RPi1Jq', 4, 1, '2026-10-06 12:53:35', '2026-10-06 12:53:35', NULL),
(15, 'CUST-4A850F', 'paul2', 'john2', 'john2@campus.edu', NULL, '$2a$10$2.8A/6QlfjWK5NXov/oAwOC.hwW25EgZxJAv0T3ZL1EPitsia3NZi', 4, 1, '2026-10-08 07:43:54', '2026-10-21 13:51:10', '2026-10-21 15:51:09'),
(16, 'CUST-D7BCC9', 'Staff', 'Member', 'staff.membead39r@eduvos.com', NULL, '$2a$10$YlWuigH30gcqv0ONpu1dgOd8vZKnCmknvUAAsy19SP/vA8kbKKNXW', 3, 1, '2026-10-08 07:58:25', '2026-10-08 07:58:25', NULL),
(17, 'CUST-27778C', 'popies', 'popie', 'pop@pops.com', NULL, '$2a$10$m72HX6SQ8ZamY/0s1belDeUH.6YgGjZ/STEq.yhXIYZeV.Po.xEOi', 4, 1, '2026-10-14 09:08:27', '2026-10-14 09:32:01', NULL),
(18, 'CUST-F91E3A', 'test2', 'test2_surname', 'test@test.com', NULL, '$2a$10$xmLii3S8N6Fd1OhWgV1uneSZPeFyZm.aPzATQijF.dOvnnrmQj1D6', 4, 1, '2026-10-14 09:11:57', '2026-10-14 09:11:57', NULL),
(19, 'CUST-61233E', 'sonia', 'Test', 'soniatest@gmail.com', NULL, '$2a$10$/tiO/DpPzfus5G6vM6VDbunfj0Hogvva7UYDrBxhA3AQXEMcKlSRG', 4, 1, '2026-10-16 14:18:20', '2026-10-16 14:18:20', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`category_id`),
  ADD UNIQUE KEY `uq_category_name` (`category_name`);

--
-- Indexes for table `category_default_groups`
--
ALTER TABLE `category_default_groups`
  ADD PRIMARY KEY (`category_default_group_id`),
  ADD UNIQUE KEY `uq_category_group` (`category_id`,`modifier_group_id`),
  ADD KEY `fk_cdg_modifier_group` (`modifier_group_id`);

--
-- Indexes for table `menu_items`
--
ALTER TABLE `menu_items`
  ADD PRIMARY KEY (`menu_item_id`),
  ADD KEY `idx_menu_items_category_id` (`category_id`);

--
-- Indexes for table `menu_item_modifier_groups`
--
ALTER TABLE `menu_item_modifier_groups`
  ADD PRIMARY KEY (`menu_item_id`,`modifier_group_id`),
  ADD KEY `fk_mimg_modifier_group` (`modifier_group_id`);

--
-- Indexes for table `modifiers`
--
ALTER TABLE `modifiers`
  ADD PRIMARY KEY (`modifier_id`),
  ADD KEY `idx_modifiers_group_id` (`modifier_group_id`);

--
-- Indexes for table `modifier_groups`
--
ALTER TABLE `modifier_groups`
  ADD PRIMARY KEY (`modifier_group_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notification_id`),
  ADD KEY `idx_notifications_user_id` (`user_id`),
  ADD KEY `idx_notifications_order_id` (`order_id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`order_id`),
  ADD UNIQUE KEY `uq_customer_order_number` (`customer_order_number`),
  ADD KEY `idx_orders_user_id` (`user_id`),
  ADD KEY `idx_orders_status_id` (`status_id`);

--
-- Indexes for table `order_items`
--
ALTER TABLE `order_items`
  ADD PRIMARY KEY (`order_item_id`),
  ADD KEY `idx_order_items_order_id` (`order_id`),
  ADD KEY `idx_order_items_menu_item_id` (`menu_item_id`);

--
-- Indexes for table `order_item_modifiers`
--
ALTER TABLE `order_item_modifiers`
  ADD PRIMARY KEY (`order_item_modifier_id`),
  ADD KEY `fk_oim_modifier` (`modifier_id`),
  ADD KEY `fk_oim_order_item` (`order_item_id`);

--
-- Indexes for table `order_status`
--
ALTER TABLE `order_status`
  ADD PRIMARY KEY (`status_id`),
  ADD UNIQUE KEY `uq_status_name` (`status_name`);

--
-- Indexes for table `order_status_history`
--
ALTER TABLE `order_status_history`
  ADD PRIMARY KEY (`status_history_id`),
  ADD KEY `idx_status_history_order_id` (`order_id`),
  ADD KEY `fk_osh_status` (`status_id`),
  ADD KEY `fk_osh_user` (`changed_by_user_id`);

--
-- Indexes for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  ADD KEY `fk_refresh_token_user` (`user_id`);

--
-- Indexes for table `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`role_id`),
  ADD UNIQUE KEY `uq_role_name` (`role_name`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `uq_email` (`email`),
  ADD UNIQUE KEY `uq_customer_code` (`customer_code`),
  ADD KEY `idx_users_role_id` (`role_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `category_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT for table `category_default_groups`
--
ALTER TABLE `category_default_groups`
  MODIFY `category_default_group_id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `menu_items`
--
ALTER TABLE `menu_items`
  MODIFY `menu_item_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=46;

--
-- AUTO_INCREMENT for table `modifiers`
--
ALTER TABLE `modifiers`
  MODIFY `modifier_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=33;

--
-- AUTO_INCREMENT for table `modifier_groups`
--
ALTER TABLE `modifier_groups`
  MODIFY `modifier_group_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `notification_id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `order_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=527;

--
-- AUTO_INCREMENT for table `order_items`
--
ALTER TABLE `order_items`
  MODIFY `order_item_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=525;

--
-- AUTO_INCREMENT for table `order_item_modifiers`
--
ALTER TABLE `order_item_modifiers`
  MODIFY `order_item_modifier_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=60;

--
-- AUTO_INCREMENT for table `order_status`
--
ALTER TABLE `order_status`
  MODIFY `status_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `order_status_history`
--
ALTER TABLE `order_status_history`
  MODIFY `status_history_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=637;

--
-- AUTO_INCREMENT for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=206;

--
-- AUTO_INCREMENT for table `roles`
--
ALTER TABLE `roles`
  MODIFY `role_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `category_default_groups`
--
ALTER TABLE `category_default_groups`
  ADD CONSTRAINT `fk_cdg_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  ADD CONSTRAINT `fk_cdg_modifier_group` FOREIGN KEY (`modifier_group_id`) REFERENCES `modifier_groups` (`modifier_group_id`);

--
-- Constraints for table `menu_items`
--
ALTER TABLE `menu_items`
  ADD CONSTRAINT `fk_menu_items_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`);

--
-- Constraints for table `menu_item_modifier_groups`
--
ALTER TABLE `menu_item_modifier_groups`
  ADD CONSTRAINT `fk_mimg_menu_item` FOREIGN KEY (`menu_item_id`) REFERENCES `menu_items` (`menu_item_id`),
  ADD CONSTRAINT `fk_mimg_modifier_group` FOREIGN KEY (`modifier_group_id`) REFERENCES `modifier_groups` (`modifier_group_id`);

--
-- Constraints for table `modifiers`
--
ALTER TABLE `modifiers`
  ADD CONSTRAINT `fk_modifiers_group` FOREIGN KEY (`modifier_group_id`) REFERENCES `modifier_groups` (`modifier_group_id`);

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `fk_orders_status` FOREIGN KEY (`status_id`) REFERENCES `order_status` (`status_id`),
  ADD CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `fk_order_items_menu_item` FOREIGN KEY (`menu_item_id`) REFERENCES `menu_items` (`menu_item_id`),
  ADD CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`);

--
-- Constraints for table `order_item_modifiers`
--
ALTER TABLE `order_item_modifiers`
  ADD CONSTRAINT `fk_oim_modifier` FOREIGN KEY (`modifier_id`) REFERENCES `modifiers` (`modifier_id`),
  ADD CONSTRAINT `fk_oim_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`order_item_id`);

--
-- Constraints for table `order_status_history`
--
ALTER TABLE `order_status_history`
  ADD CONSTRAINT `fk_osh_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
  ADD CONSTRAINT `fk_osh_status` FOREIGN KEY (`status_id`) REFERENCES `order_status` (`status_id`),
  ADD CONSTRAINT `fk_osh_user` FOREIGN KEY (`changed_by_user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `refresh_tokens`
--
ALTER TABLE `refresh_tokens`
  ADD CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_roles` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
