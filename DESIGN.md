# Design

## System Overview

Ciallo～(∠・ω< )⌒☆ 博客系统是保留原 Hexo 主题氛围的产品型界面。视觉重点是温暖橙色、半透明白色内容层、动漫背景图和紧凑清晰的工具组件。页面不追求营销感，而是让登录、文章、审核、评论和后台管理流程更容易理解。

## Foundations

### Color

- Primary: `#e67249` from the theme, used for primary actions, active states and important status text.
- Primary hover / login accent: `#f06c45` and `#ff8b66`, used only for action emphasis.
- Surface: `var(--color-card)` / `rgba(255,255,255,.82-.92)` for panels above the existing background image.
- Text primary: `var(--color-text-1)` / `#111` for all important body text.
- Text secondary: `var(--color-text-2)` / `#444` for metadata and helper text.
- Status colors: green for approved/published, amber for pending, red for rejected/danger, neutral for draft.

### Typography

Use the existing system sans stack: `-apple-system, BlinkMacSystemFont, Segoe UI, Microsoft YaHei, sans-serif`. Keep product type compact: h2 around 20-22px, body 14-16px, metadata 13px. Avoid fluid display type and uppercase tracking.

### Layout

Use a single-column app shell with constrained panels, responsive `auto-fit` grids, and dense toolbars that wrap cleanly on mobile. Cards are used for repeated articles, comments, users and stats only. Avoid nested cards.

### Motion

Use short state transitions: color, transform, background and border changes at 160-220ms. Do not animate page layout. Respect `prefers-reduced-motion: reduce` by disabling transforms.

## Components

### Login Card

Standalone page, no theme drawer. Background image remains visible through a warm overlay. The card is centered, readable, and focused on one task: login or register. Demo credentials are present but visually secondary.

### Toolbar

Horizontal on desktop, stacked on mobile. Inputs and selects share the same border radius, height and focus ring. Primary and danger actions must be visually distinct.

### Panels

Panels group one workflow each: homepage feed, my articles, editor, admin users, article review, comment review. Surface is quiet and readable with a single border or soft shadow, never both heavily.

### Article Cards

Cards show status, title, metadata, summary, tags and actions in a predictable order. Public feed cards emphasize reading, liking and commenting. My-article cards emphasize edit/delete and current approval state.

### Admin Rows

Admin rows are denser than article cards. They should expose id, title/user/comment, status, supporting metadata and action buttons without turning into nested cards.

### Status Pills

Rounded pills are allowed for compact state labels. Keep color subtle and text high contrast. States: 已上架/已通过, 待审核, 已驳回, 草稿, 已封禁, 正常.

## Responsive Rules

At narrow widths, toolbars and forms stack vertically, buttons fill width, grids collapse to one column, and long text wraps. The article editor and admin action clusters must remain usable without horizontal scrolling.
