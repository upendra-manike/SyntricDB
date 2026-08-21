from setuptools import setup, find_packages

setup(
    name="syntricdb-client",
    version="1.0.0",
    description="Official Python Client for SyntricDB AI-Native Unified Database Engine",
    long_description=open("README.md").read(),
    long_description_content_type="text/markdown",
    author="Upendra Manike",
    author_email="upendra@syntricdb.com",
    url="https://github.com/upendra-manike/SyntricDB",
    packages=find_packages(),
    install_requires=[
        "requests>=2.28.0",
    ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        "Topic :: Database",
        "Topic :: Scientific/Engineering :: Artificial Intelligence",
    ],
    python_requires=">=3.8",
)
