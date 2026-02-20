from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 720})

        # Go to home page
        page.goto("http://localhost:5173")
        page.wait_for_timeout(2000)
        page.screenshot(path="verification/home.png")
        print("Home page screenshot taken.")

        # Go to auth callback page
        page.goto("http://localhost:5173/confirm")
        page.wait_for_timeout(2000)
        page.screenshot(path="verification/confirm.png")
        print("Confirm page screenshot taken.")

        browser.close()

if __name__ == "__main__":
    run()
