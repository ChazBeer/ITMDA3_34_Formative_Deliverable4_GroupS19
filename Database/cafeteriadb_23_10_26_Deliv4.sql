-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 24, 2026 at 10:59 AM
-- Server version: 10.11.16-MariaDB
-- PHP Version: 8.5.12

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
(11, 14, 'Breakfast Wrap', 'A toasted wrap filled with scrambled egg.', 40.00, 1, 'https://res.cloudinary.com/qpskyxx6/image/upload/v1792163085/edueats/menu-items/0172f1cf-6cda-4e22-b9f9-e9562d3d39a8.jpg', 2, '2026-10-21 13:48:15', '2026-10-24 08:58:49', 29),
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

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`order_id`, `user_id`, `status_id`, `customer_order_number`, `collected_at`, `total_price`, `created_at`, `updated_at`) VALUES
(527, 22, 1, 'ORD-10-24-YUUQ', NULL, 40.00, '2026-10-24 08:58:49', '2026-10-24 08:58:49');

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

--
-- Dumping data for table `order_items`
--

INSERT INTO `order_items` (`order_item_id`, `order_id`, `menu_item_id`, `quantity`, `menu_item_name_at_purchase`, `price_at_purchase`, `special_instructions`, `created_at`, `updated_at`) VALUES
(525, 527, 11, 1, 'Breakfast Wrap', 40.00, '', '2026-10-24 08:58:49', '2026-10-24 08:58:49');

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

--
-- Dumping data for table `order_item_modifiers`
--

INSERT INTO `order_item_modifiers` (`order_item_modifier_id`, `order_item_id`, `modifier_id`, `modifier_name_at_purchase`, `price_at_purchase`, `created_at`) VALUES
(60, 525, 12, 'Mayo', 0.00, '2026-10-24 08:58:49'),
(61, 525, 15, 'No tomato', 0.00, '2026-10-24 08:58:49');

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
(206, 'adffc57bfa56971c91aa4217cd62e5cce2cb44dfe56752dfa9b8ddeefc0a8b48', 20, '2026-10-31 08:57:26.000000', 1, '2026-10-24 08:57:26.000000'),
(207, '339aea0baf1fbe4e59b767243225556e0ea4eeb64fcd903ffd199185fc8fef83', 22, '2026-10-31 08:58:21.000000', 0, '2026-10-24 08:58:21.000000');

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
(20, 'CUST-ADM-01', 'System', 'Administrator', 'admin@campus.edu', '+15550101', '$2a$12$OZJUEBBMpc/fqv7tKsRI/euZOMSj7nUk5rjR7hX11JxCQBHq0TlBq', 1, 1, '2026-10-24 08:56:01', '2026-10-24 08:57:26', '2026-10-24 10:57:26'),
(21, 'CUST-STF-02', 'Campus', 'Staff', 'staff@campus.edu', '+15550102', '$2a$12$gU2nXTHKxJnaVRsew48H3uKI9Zl37F6/k1rjXZ2tuhuiNteLDDdR2', 2, 1, '2026-10-24 08:56:01', '2026-10-24 08:56:01', NULL),
(22, 'CUST-CUS-03', 'John', 'Doe', 'customer@campus.edu', '+15550103', '$2a$12$NJfdsxahy/UBQCwaVMOpIeMbnEe6NDZi1WYp6n0snvWOaVcfNP2tq', 4, 1, '2026-10-24 08:56:01', '2026-10-24 08:58:21', '2026-10-24 10:58:21');

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
  MODIFY `order_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=528;

--
-- AUTO_INCREMENT for table `order_items`
--
ALTER TABLE `order_items`
  MODIFY `order_item_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=526;

--
-- AUTO_INCREMENT for table `order_item_modifiers`
--
ALTER TABLE `order_item_modifiers`
  MODIFY `order_item_modifier_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

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
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=208;

--
-- AUTO_INCREMENT for table `roles`
--
ALTER TABLE `roles`
  MODIFY `role_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

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
