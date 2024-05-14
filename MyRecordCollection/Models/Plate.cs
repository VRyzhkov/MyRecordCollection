using System;
using System.Collections.Generic;

namespace MyRecordCollection.Models
{
    public class Plate
    {
        public Guid Id { get; set; }

        public string Title { get; set; }

        public int Year { get; set; }

        public string ImageUrl { get; set; }

        public List<Track> Tracks { get; set; }
    }
}